package dev.lenvx.gateway.inventory


enum class ClickType {
    
    LEFT,
    
    SHIFT_LEFT,
    
    RIGHT,
    
    SHIFT_RIGHT,
    
    WINDOW_BORDER_LEFT,
    
    WINDOW_BORDER_RIGHT,
    
    MIDDLE,
    
    NUMBER_KEY,
    
    DOUBLE_CLICK,
    
    DROP,
    
    CONTROL_DROP,
    
    CREATIVE,
    
    SWAP_OFFHAND,
    
    UNKNOWN,
    ;

    
    fun isKeyboardClick(): Boolean {
        return (this == NUMBER_KEY) || (this == DROP) || (this == CONTROL_DROP)
    }

    
    fun isCreativeAction(): Boolean {
        return (this == MIDDLE) || (this == CREATIVE)
    }

    
    fun isRightClick(): Boolean {
        return (this == RIGHT) || (this == SHIFT_RIGHT)
    }

    
    fun isLeftClick(): Boolean {
        return (this == LEFT) || (this == SHIFT_LEFT) || (this == DOUBLE_CLICK) || (this == CREATIVE)
    }

    
    fun isShiftClick(): Boolean {
        return (this == SHIFT_LEFT) || (this == SHIFT_RIGHT) || (this == CONTROL_DROP)
    }
}

