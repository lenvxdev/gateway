package dev.lenvx.gateway.metrics

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.file.FileConfiguration
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection


@Suppress("UNCHECKED_CAST")
class Metrics @Throws(IOException::class) constructor() {

    companion object {
        private const val SERVER_SOFTWARE = "Gateway"
        const val B_STATS_VERSION = 1
        private const val URL = "https://bStats.org/submitData/server-implementation"
        private var logFailedRequests = false

        
        @Throws(Exception::class)
        private fun sendData(data: JSONObject?) {
            requireNotNull(data) { "Data cannot be null!" }
            val connection = URL(URL).openConnection() as HttpsURLConnection
            val compressedData = compress(data.toString()) ?: return
            connection.requestMethod = "POST"
            connection.addRequestProperty("Accept", "application/json")
            connection.addRequestProperty("Connection", "close")
            connection.addRequestProperty("Content-Encoding", "gzip")
            connection.addRequestProperty("Content-Length", compressedData.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "MC-Server/$B_STATS_VERSION")
            connection.doOutput = true
            DataOutputStream(connection.outputStream).use { outputStream ->
                outputStream.write(compressedData)
                outputStream.flush()
            }

            connection.inputStream.close()
        }

        
        @Throws(IOException::class)
        private fun compress(str: String?): ByteArray? {
            if (str == null) {
                return null
            }
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use { gzip ->
                gzip.write(str.toByteArray(StandardCharsets.UTF_8))
            }
            return outputStream.toByteArray()
        }
    }
    private val serverUUID: String?
    private val charts: MutableList<CustomChart> = ArrayList()

    private val maxPlayerCountInPeriod = AtomicInteger(0)

    init {
        val configFile = File("plugins/bStats", "config.yml")
        val config = FileConfiguration(configFile)
        if (config.get("serverUuid", String::class.java) == null) {
            config.set("enabled", true)
            config.set("serverUuid", UUID.randomUUID().toString())
            config.set("logFailedRequests", false)
            config.setHeader(
                "bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
                        "To honor their work, you should not disable it.\n" +
                        "This has nearly no effect on the server performance!\n" +
                        "Check out https://bStats.org/ to learn more :)"
            )
            try {
                config.saveConfig(configFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        serverUUID = config.get("serverUuid", String::class.java)
        logFailedRequests = config.get("logFailedRequests", Boolean::class.java) ?: false
        if (config.get("enabled", Boolean::class.java) == true) {
            startSubmitting()
        }

        addCustomChart(SingleLineChart("players") { maxPlayerCountInPeriod.getAndSet(0) })
        addCustomChart(SimplePie("gateway_version") { Gateway.instance!!.GATEWAY_IMPLEMENTATION_VERSION })
        addCustomChart(SimplePie("minecraftVersion") { Gateway.instance!!.SERVER_IMPLEMENTATION_VERSION })
    }

    
    fun updatePlayersCount() {
        maxPlayerCountInPeriod.getAndUpdate { i -> Math.max(i, Gateway.instance!!.players.size) }
    }

    
    fun addCustomChart(chart: CustomChart?) {
        requireNotNull(chart) { "Chart cannot be null!" }
        charts.add(chart)
    }

    
    private fun startSubmitting() {
        val timer = Timer(true)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                submitData()
            }
        }, (1000 * 60 * 5).toLong(), (1000 * 60 * 30).toLong())
    }

    
    private fun getPluginData(): JSONObject {
        val data = JSONObject()

        data["pluginName"] = SERVER_SOFTWARE
        val customCharts = JSONArray()
        for (customChart in charts) {
            val chart = customChart.requestJsonObject
            if (chart == null) {
                continue
            }
            customCharts.add(chart)
        }
        data["customCharts"] = customCharts

        return data
    }

    
    private fun getServerData(): JSONObject {
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val osVersion = System.getProperty("os.version")
        val coreCount = Runtime.getRuntime().availableProcessors()

        val data = JSONObject()

        data["serverUUID"] = serverUUID

        data["playerAmount"] = Gateway.instance!!.players.size
        data["osName"] = osName
        data["osArch"] = osArch
        data["osVersion"] = osVersion
        data["coreCount"] = coreCount

        return data
    }

    
    private fun submitData() {
        val data = getServerData()

        val pluginData = JSONArray()
        pluginData.add(getPluginData())
        data["plugins"] = pluginData

        try {
            sendData(data)
        } catch (e: Exception) {
            if (logFailedRequests) {
                Gateway.instance!!.console.sendMessage("Could not submit stats of $SERVER_SOFTWARE\n$e")
            }
        }
    }

    
    abstract class CustomChart(
        val chartId: String
    ) {

        init {
            require(chartId.isNotEmpty()) { "ChartId cannot be null or empty!" }
        }

        val requestJsonObject: JSONObject?
            get() {
                val chart = JSONObject()
                chart["chartId"] = chartId
                try {
                    val data = chartData
                    if (data == null) {
                        return null
                    }
                    chart["data"] = data
                } catch (t: Throwable) {
                    if (logFailedRequests) {
                        Gateway.instance!!.console.sendMessage("Failed to get data for custom chart with id $chartId\n$t")
                    }
                    return null
                }
                return chart
            }

        @get:Throws(Exception::class)
        protected abstract val chartData: JSONObject?

    }

    
    class SimplePie(chartId: String, private val callable: Callable<String?>) : CustomChart(chartId) {

        @get:Throws(Exception::class)
        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val value = callable.call()
                if (value == null || value.isEmpty()) {
                    return null
                }
                data["value"] = value
                return data
            }
    }

    
    class AdvancedPie(chartId: String, private val callable: Callable<Map<String, Int>?>) : CustomChart(chartId) {

        @get:Throws(Exception::class)
        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val values = JSONObject()
                val map = callable.call()
                if (map == null || map.isEmpty()) {
                    return null
                }
                var allSkipped = true
                for ((key, value) in map) {
                    if (value == 0) {
                        continue
                    }
                    allSkipped = false
                    values[key] = value
                }
                if (allSkipped) {
                    return null
                }
                data["values"] = values
                return data
            }
    }

    
    class DrilldownPie(chartId: String, private val callable: Callable<Map<String, Map<String, Int>>?>) :
        CustomChart(chartId) {

        @get:Throws(Exception::class)
        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val values = JSONObject()
                val map = callable.call()
                if (map == null || map.isEmpty()) {
                    return null
                }
                var reallyAllSkipped = true
                for ((key, value1) in map) {
                    val value = JSONObject()
                    var allSkipped = true
                    for ((key1, value2) in value1) {
                        value[key1] = value2
                        allSkipped = false
                    }
                    if (!allSkipped) {
                        reallyAllSkipped = false
                        values[key] = value
                    }
                }
                if (reallyAllSkipped) {
                    return null
                }
                data["values"] = values
                return data
            }
    }

    
    class SingleLineChart(chartId: String, private val callable: Callable<Int?>) : CustomChart(chartId) {

        @get:Throws(Exception::class)
        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val value = callable.call() ?: 0
                if (value == 0) {
                    return null
                }
                data["value"] = value
                return data
            }

    }

    
    class MultiLineChart(chartId: String, private val callable: Callable<Map<String, Int>?>) : CustomChart(chartId) {

        @get:Throws(Exception::class)
        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val values = JSONObject()
                val map = callable.call()
                if (map == null || map.isEmpty()) {
                    return null
                }
                var allSkipped = true
                for ((key, value) in map) {
                    if (value == 0) {
                        continue
                    }
                    allSkipped = false
                    values[key] = value
                }
                if (allSkipped) {
                    return null
                }
                data["values"] = values
                return data
            }

    }

    
    class SimpleBarChart(chartId: String, private val callable: Callable<Map<String, Int>?>) : CustomChart(chartId) {

        @get:Throws(Exception::class)
        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val values = JSONObject()
                val map = callable.call()
                if (map == null || map.isEmpty()) {
                    return null
                }
                for ((key, value) in map) {
                    val categoryValues = JSONArray()
                    categoryValues.add(value)
                    values[key] = categoryValues
                }
                data["values"] = values
                return data
            }

    }

    
    class AdvancedBarChart(chartId: String, private val callable: Callable<Map<String, IntArray>?>) :
        CustomChart(chartId) {

        @get:Throws(Exception::class)
        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val values = JSONObject()
                val map = callable.call()
                if (map == null || map.isEmpty()) {
                    return null
                }
                var allSkipped = true
                for ((key, value) in map) {
                    if (value.isEmpty()) {
                        continue
                    }
                    allSkipped = false
                    val categoryValues = JSONArray()
                    for (categoryValue in value) {
                        categoryValues.add(categoryValue)
                    }
                    values[key] = categoryValues
                }
                if (allSkipped) {
                    return null
                }
                data["values"] = values
                return data
            }

    }

    
    abstract class SimpleMapChart(chartId: String) : CustomChart(chartId) {

        
        abstract val value: Country?

        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val value = value ?: return null
                data["value"] = value.countryIsoTag
                return data
            }

    }

    
    abstract class AdvancedMapChart(chartId: String) : CustomChart(chartId) {

        
        abstract fun getValues(valueMap: HashMap<Country, Int>): HashMap<Country, Int>?

        override val chartData: JSONObject?
            get() {
                val data = JSONObject()
                val values = JSONObject()
                val map = getValues(HashMap())
                if (map == null || map.isEmpty()) {
                    return null
                }
                var allSkipped = true
                for ((key, value) in map) {
                    if (value == 0) {
                        continue
                    }
                    allSkipped = false
                    values[key.countryIsoTag] = value
                }
                if (allSkipped) {
                    return null
                }
                data["values"] = values
                return data
            }

    }

    
    enum class Country(
        
        val countryIsoTag: String,
        
        val countryName: String
    ) {

        
        AUTO_DETECT("AUTO", "Auto Detected"),

        ANDORRA("AD", "Andorra"),
        UNITED_ARAB_EMIRATES("AE", "United Arab Emirates"),
        AFGHANISTAN("AF", "Afghanistan"),
        ANTIGUA_AND_BARBUDA("AG", "Antigua and Barbuda"),
        ANGUILLA("AI", "Anguilla"),
        ALBANIA("AL", "Albania"),
        ARMENIA("AM", "Armenia"),
        NETHERLANDS_ANTILLES("AN", "Netherlands Antilles"),
        ANGOLA("AO", "Angola"),
        ANTARCTICA("AQ", "Antarctica"),
        ARGENTINA("AR", "Argentina"),
        AMERICAN_SAMOA("AS", "American Samoa"),
        AUSTRIA("AT", "Austria"),
        AUSTRALIA("AU", "Australia"),
        ARUBA("AW", "Aruba"),
        ALAND_ISLANDS("AX", "land Islands"),
        AZERBAIJAN("AZ", "Azerbaijan"),
        BOSNIA_AND_HERZEGOVINA("BA", "Bosnia and Herzegovina"),
        BARBADOS("BB", "Barbados"),
        BANGLADESH("BD", "Bangladesh"),
        BELGIUM("BE", "Belgium"),
        BURKINA_FASO("BF", "Burkina Faso"),
        BULGARIA("BG", "Bulgaria"),
        BAHRAIN("BH", "Bahrain"),
        BURUNDI("BI", "Burundi"),
        BENIN("BJ", "Benin"),
        SAINT_BARTHELEMY("BL", "Saint Barthlemy"),
        BERMUDA("BM", "Bermuda"),
        BRUNEI("BN", "Brunei"),
        BOLIVIA("BO", "Bolivia"),
        BONAIRE_SINT_EUSTATIUS_AND_SABA("BQ", "Bonaire, Sint Eustatius and Saba"),
        BRAZIL("BR", "Brazil"),
        BAHAMAS("BS", "Bahamas"),
        BHUTAN("BT", "Bhutan"),
        BOUVET_ISLAND("BV", "Bouvet Island"),
        BOTSWANA("BW", "Botswana"),
        BELARUS("BY", "Belarus"),
        BELIZE("BZ", "Belize"),
        CANADA("CA", "Canada"),
        COCOS_ISLANDS("CC", "Cocos Islands"),
        THE_DEMOCRATIC_REPUBLIC_OF_CONGO("CD", "The Democratic Republic Of Congo"),
        CENTRAL_AFRICAN_REPUBLIC("CF", "Central African Republic"),
        CONGO("CG", "Congo"),
        SWITZERLAND("CH", "Switzerland"),
        COTE_D_IVOIRE("CI", "Cte d'Ivoire"),
        COOK_ISLANDS("CK", "Cook Islands"),
        CHILE("CL", "Chile"),
        CAMEROON("CM", "Cameroon"),
        CHINA("CN", "China"),
        COLOMBIA("CO", "Colombia"),
        COSTA_RICA("CR", "Costa Rica"),
        CUBA("CU", "Cuba"),
        CAPE_VERDE("CV", "Cape Verde"),
        CURACAO("CW", "Curaao"),
        CHRISTMAS_ISLAND("CX", "Christmas Island"),
        CYPRUS("CY", "Cyprus"),
        CZECH_REPUBLIC("CZ", "Czech Republic"),
        GERMANY("DE", "Germany"),
        DJIBOUTI("DJ", "Djibouti"),
        DENMARK("DK", "Denmark"),
        DOMINICA("DM", "Dominica"),
        DOMINICAN_REPUBLIC("DO", "Dominican Republic"),
        ALGERIA("DZ", "Algeria"),
        ECUADOR("EC", "Ecuador"),
        ESTONIA("EE", "Estonia"),
        EGYPT("EG", "Egypt"),
        WESTERN_SAHARA("EH", "Western Sahara"),
        ERITREA("ER", "Eritrea"),
        SPAIN("ES", "Spain"),
        ETHIOPIA("ET", "Ethiopia"),
        FINLAND("FI", "Finland"),
        FIJI("FJ", "Fiji"),
        FALKLAND_ISLANDS("FK", "Falkland Islands"),
        MICRONESIA("FM", "Micronesia"),
        FAROE_ISLANDS("FO", "Faroe Islands"),
        FRANCE("FR", "France"),
        GABON("GA", "Gabon"),
        UNITED_KINGDOM("GB", "United Kingdom"),
        GRENADA("GD", "Grenada"),
        GEORGIA("GE", "Georgia"),
        FRENCH_GUIANA("GF", "French Guiana"),
        GUERNSEY("GG", "Guernsey"),
        GHANA("GH", "Ghana"),
        GIBRALTAR("GI", "Gibraltar"),
        GREENLAND("GL", "Greenland"),
        GAMBIA("GM", "Gambia"),
        GUINEA("GN", "Guinea"),
        GUADELOUPE("GP", "Guadeloupe"),
        EQUATORIAL_GUINEA("GQ", "Equatorial Guinea"),
        GREECE("GR", "Greece"),
        SOUTH_GEORGIA_AND_THE_SOUTH_SANDWICH_ISLANDS("GS", "South Georgia And The South Sandwich Islands"),
        GUATEMALA("GT", "Guatemala"),
        GUAM("GU", "Guam"),
        GUINEA_BISSAU("GW", "Guinea-Bissau"),
        GUYANA("GY", "Guyana"),
        HONG_KONG("HK", "Hong Kong"),
        HEARD_ISLAND_AND_MCDONALD_ISLANDS("HM", "Heard Island And McDonald Islands"),
        HONDURAS("HN", "Honduras"),
        CROATIA("HR", "Croatia"),
        HAITI("HT", "Haiti"),
        HUNGARY("HU", "Hungary"),
        INDONESIA("ID", "Indonesia"),
        IRELAND("IE", "Ireland"),
        ISRAEL("IL", "Israel"),
        ISLE_OF_MAN("IM", "Isle Of Man"),
        INDIA("IN", "India"),
        BRITISH_INDIAN_OCEAN_TERRITORY("IO", "British Indian Ocean Territory"),
        IRAQ("IQ", "Iraq"),
        IRAN("IR", "Iran"),
        ICELAND("IS", "Iceland"),
        ITALY("IT", "Italy"),
        JERSEY("JE", "Jersey"),
        JAMAICA("JM", "Jamaica"),
        JORDAN("JO", "Jordan"),
        JAPAN("JP", "Japan"),
        KENYA("KE", "Kenya"),
        KYRGYZSTAN("KG", "Kyrgyzstan"),
        CAMBODIA("KH", "Cambodia"),
        KIRIBATI("KI", "Kiribati"),
        COMOROS("KM", "Comoros"),
        SAINT_KITTS_AND_NEVIS("KN", "Saint Kitts And Nevis"),
        NORTH_KOREA("KP", "North Korea"),
        SOUTH_KOREA("KR", "South Korea"),
        KUWAIT("KW", "Kuwait"),
        CAYMAN_ISLANDS("KY", "Cayman Islands"),
        KAZAKHSTAN("KZ", "Kazakhstan"),
        LAOS("LA", "Laos"),
        LEBANON("LB", "Lebanon"),
        SAINT_LUCIA("LC", "Saint Lucia"),
        LIECHTENSTEIN("LI", "Liechtenstein"),
        SRI_LANKA("LK", "Sri Lanka"),
        LIBERIA("LR", "Liberia"),
        LESOTHO("LS", "Lesotho"),
        LITHUANIA("LT", "Lithuania"),
        LUXEMBOURG("LU", "Luxembourg"),
        LATVIA("LV", "Latvia"),
        LIBYA("LY", "Libya"),
        MOROCCO("MA", "Morocco"),
        MONACO("MC", "Monaco"),
        MOLDOVA("MD", "Moldova"),
        MONTENEGRO("ME", "Montenegro"),
        SAINT_MARTIN("MF", "Saint Martin"),
        MADAGASCAR("MG", "Madagascar"),
        MARSHALL_ISLANDS("MH", "Marshall Islands"),
        MACEDONIA("MK", "Macedonia"),
        MALI("ML", "Mali"),
        MYANMAR("MM", "Myanmar"),
        MONGOLIA("MN", "Mongolia"),
        MACAO("MO", "Macao"),
        NORTHERN_MARIANA_ISLANDS("MP", "Northern Mariana Islands"),
        MARTINIQUE("MQ", "Martinique"),
        MAURITANIA("MR", "Mauritania"),
        MONTSERRAT("MS", "Montserrat"),
        MALTA("MT", "Malta"),
        MAURITIUS("MU", "Mauritius"),
        MALDIVES("MV", "Maldives"),
        MALAWI("MW", "Malawi"),
        MEXICO("MX", "Mexico"),
        MALAYSIA("MY", "Malaysia"),
        MOZAMBIQUE("MZ", "Mozambique"),
        NAMIBIA("NA", "Namibia"),
        NEW_CALEDONIA("NC", "New Caledonia"),
        NIGER("NE", "Niger"),
        NORFOLK_ISLAND("NF", "Norfolk Island"),
        NIGERIA("NG", "Nigeria"),
        NICARAGUA("NI", "Nicaragua"),
        NETHERLANDS("NL", "Netherlands"),
        NORWAY("NO", "Norway"),
        NEPAL("NP", "Nepal"),
        NAURU("NR", "Nauru"),
        NIUE("NU", "Niue"),
        NEW_ZEALAND("NZ", "New Zealand"),
        OMAN("OM", "Oman"),
        PANAMA("PA", "Panama"),
        PERU("PE", "Peru"),
        FRENCH_POLYNESIA("PF", "French Polynesia"),
        PAPUA_NEW_GUINEA("PG", "Papua New Guinea"),
        PHILIPPINES("PH", "Philippines"),
        PAKISTAN("PK", "Pakistan"),
        POLAND("PL", "Poland"),
        SAINT_PIERRE_AND_MIQUELON("PM", "Saint Pierre And Miquelon"),
        PITCAIRN("PN", "Pitcairn"),
        PUERTO_RICO("PR", "Puerto Rico"),
        PALESTINE("PS", "Palestine"),
        PORTUGAL("PT", "Portugal"),
        PALAU("PW", "Palau"),
        PARAGUAY("PY", "Paraguay"),
        QATAR("QA", "Qatar"),
        REUNION("RE", "Reunion"),
        ROMANIA("RO", "Romania"),
        SERBIA("RS", "Serbia"),
        RUSSIA("RU", "Russia"),
        RWANDA("RW", "Rwanda"),
        SAUDI_ARABIA("SA", "Saudi Arabia"),
        SOLOMON_ISLANDS("SB", "Solomon Islands"),
        SEYCHELLES("SC", "Seychelles"),
        SUDAN("SD", "Sudan"),
        SWEDEN("SE", "Sweden"),
        SINGAPORE("SG", "Singapore"),
        SAINT_HELENA("SH", "Saint Helena"),
        SLOVENIA("SI", "Slovenia"),
        SVALBARD_AND_JAN_MAYEN("SJ", "Svalbard And Jan Mayen"),
        SLOVAKIA("SK", "Slovakia"),
        SIERRA_LEONE("SL", "Sierra Leone"),
        SAN_MARINO("SM", "San Marino"),
        SENEGAL("SN", "Senegal"),
        SOMALIA("SO", "Somalia"),
        SURINAME("SR", "Suriname"),
        SOUTH_SUDAN("SS", "South Sudan"),
        SAO_TOME_AND_PRINCIPE("ST", "Sao Tome And Principe"),
        EL_SALVADOR("SV", "El Salvador"),
        SINT_MAARTEN_DUTCH_PART("SX", "Sint Maarten (Dutch part)"),
        SYRIA("SY", "Syria"),
        SWAZILAND("SZ", "Swaziland"),
        TURKS_AND_CAICOS_ISLANDS("TC", "Turks And Caicos Islands"),
        CHAD("TD", "Chad"),
        FRENCH_SOUTHERN_TERRITORIES("TF", "French Southern Territories"),
        TOGO("TG", "Togo"),
        THAILAND("TH", "Thailand"),
        TAJIKISTAN("TJ", "Tajikistan"),
        TOKELAU("TK", "Tokelau"),
        TIMOR_LESTE("TL", "Timor-Leste"),
        TURKMENISTAN("TM", "Turkmenistan"),
        TUNISIA("TN", "Tunisia"),
        TONGA("TO", "Tonga"),
        TURKEY("TR", "Turkey"),
        TRINIDAD_AND_TOBAGO("TT", "Trinidad and Tobago"),
        TUVALU("TV", "Tuvalu"),
        TAIWAN("TW", "Taiwan"),
        TANZANIA("TZ", "Tanzania"),
        UKRAINE("UA", "Ukraine"),
        UGANDA("UG", "Uganda"),
        UNITED_STATES_MINOR_OUTLYING_ISLANDS("UM", "United States Minor Outlying Islands"),
        UNITED_STATES("US", "United States"),
        URUGUAY("UY", "Uruguay"),
        UZBEKISTAN("UZ", "Uzbekistan"),
        VATICAN("VA", "Vatican"),
        SAINT_VINCENT_AND_THE_GRENADINES("VC", "Saint Vincent And The Grenadines"),
        VENEZUELA("VE", "Venezuela"),
        BRITISH_VIRGIN_ISLANDS("VG", "British Virgin Islands"),
        U_S__VIRGIN_ISLANDS("VI", "U.S. Virgin Islands"),
        VIETNAM("VN", "Vietnam"),
        VANUATU("VU", "Vanuatu"),
        WALLIS_AND_FUTUNA("WF", "Wallis And Futuna"),
        SAMOA("WS", "Samoa"),
        YEMEN("YE", "Yemen"),
        MAYOTTE("YT", "Mayotte"),
        SOUTH_AFRI_CA("ZA", "South Africa"),
        ZAMBIA("ZM", "Zambia"),
        ZIMBABWE("ZW", "Zimbabwe");

        companion object {
            
            @JvmStatic
            fun byIsoTag(isoTag: String): Country? {
                for (country in values()) {
                    if (country.countryIsoTag == isoTag) {
                        return country
                    }
                }
                return null
            }

            
            @JvmStatic
            fun byLocale(locale: Locale): Country? {
                return byIsoTag(locale.country)
            }
        }
    }
}




