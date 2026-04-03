#!/usr/bin/env python3
import json
import socket
import struct
import sys
from typing import Tuple


def write_varint(value: int) -> bytes:
    out = bytearray()
    value &= 0xFFFFFFFF
    while True:
        temp = value & 0x7F
        value >>= 7
        if value != 0:
            temp |= 0x80
        out.append(temp)
        if value == 0:
            break
    return bytes(out)


def read_varint(sock: socket.socket) -> int:
    num_read = 0
    result = 0
    while True:
        raw = sock.recv(1)
        if not raw:
            raise RuntimeError("Unexpected EOF while reading VarInt")
        byte = raw[0]
        result |= (byte & 0x7F) << (7 * num_read)
        num_read += 1
        if num_read > 5:
            raise RuntimeError("VarInt too long")
        if (byte & 0x80) == 0:
            break
    return result


def read_exact(sock: socket.socket, length: int) -> bytes:
    data = bytearray()
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise RuntimeError("Unexpected EOF while reading payload")
        data.extend(chunk)
    return bytes(data)


def build_handshake_packet(host: str, port: int, protocol: int = 774) -> bytes:
    host_bytes = host.encode("utf-8")
    packet = bytearray()
    packet.extend(write_varint(0x00))
    packet.extend(write_varint(protocol))
    packet.extend(write_varint(len(host_bytes)))
    packet.extend(host_bytes)
    packet.extend(struct.pack(">H", port))
    packet.extend(write_varint(1))
    return write_varint(len(packet)) + bytes(packet)


def build_status_request_packet() -> bytes:
    inner = write_varint(0x00)
    return write_varint(len(inner)) + inner


def parse_status_response(payload: bytes) -> dict:
    idx = 0

    def read_varint_from_bytes() -> int:
        nonlocal idx
        num_read = 0
        result = 0
        while True:
            if idx >= len(payload):
                raise RuntimeError("Unexpected EOF in status payload")
            byte = payload[idx]
            idx += 1
            result |= (byte & 0x7F) << (7 * num_read)
            num_read += 1
            if num_read > 5:
                raise RuntimeError("VarInt too long in status payload")
            if (byte & 0x80) == 0:
                break
        return result

    packet_id = read_varint_from_bytes()
    if packet_id != 0x00:
        raise RuntimeError(f"Unexpected status packet id: {packet_id}")

    json_len = read_varint_from_bytes()
    if idx + json_len > len(payload):
        raise RuntimeError("Invalid JSON length in status payload")
    raw_json = payload[idx:idx + json_len].decode("utf-8")
    return json.loads(raw_json)


def probe(host: str, port: int, timeout_sec: float = 5.0) -> Tuple[str, str]:
    with socket.create_connection((host, port), timeout=timeout_sec) as sock:
        sock.settimeout(timeout_sec)
        sock.sendall(build_handshake_packet(host, port))
        sock.sendall(build_status_request_packet())

        packet_len = read_varint(sock)
        payload = read_exact(sock, packet_len)
        status = parse_status_response(payload)
        version = status.get("version", {}).get("name", "unknown")
        motd = status.get("description")
        return version, json.dumps(motd, ensure_ascii=False)


def main() -> int:
    if len(sys.argv) < 3:
        print("Usage: e2e_status_probe.py <host> <port>", file=sys.stderr)
        return 2
    host = sys.argv[1]
    port = int(sys.argv[2])
    version, motd = probe(host, port)
    print(f"ok host={host} port={port} version={version} motd={motd}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

