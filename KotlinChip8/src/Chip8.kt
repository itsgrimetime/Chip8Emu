import java.io.File
import java.awt.Frame
import java.awt.Graphics
import java.awt.Color
import java.awt.event.WindowListener
import java.awt.event.WindowEvent
import java.awt.event.WindowAdapter

class Chip8(filename: String) {

    final val debug : Boolean = false;

    val screen : Chip8Screen = Chip8Screen()

    val filename : String = filename

    /* memory, 4096 bytes */
    var mem   : ByteArray = ByteArray(4096)

    /* Our registers, reg[0 - 15] correspond to v0 - F */
    var reg   : ByteArray = ByteArray(16)

    /* vI is a special register used by certain instructions */
    var vI    : Byte = 0

    /* sound and delay registers - when non-zero they are automatically decremented at a rate of 60hz */
    var sound : Byte = 0
    var delay : Byte = 0

    /* 16-bit Program counter */
    var pc    : Short = 0

    /* stack pointer */
    var sp    : Byte = 0

    /* stack of 16 16-bit values */
    var stack : ShortArray = ShortArray(16)

    /* character sprites (0 - 9, a - f) */
    val sprites : Array<Array<Int>> = array(
            array(0xF0, 0x90, 0x90, 0x90, 0xF0),
            array(0x20, 0x60, 0x20, 0x20, 0x70),
            array(0xF0, 0x10, 0xF0, 0x80, 0xF0),
            array(0xF0, 0x10, 0xF0, 0x10, 0xF0),
            array(0x90, 0x90, 0xF0, 0x10, 0x10),
            array(0xF0, 0x80, 0xF0, 0x10, 0xF0),
            array(0xF0, 0x80, 0xF0, 0x90, 0xF0),
            array(0xF0, 0x10, 0x20, 0x40, 0x40),
            array(0xF0, 0x90, 0xF0, 0x90, 0xF0),
            array(0xF0, 0x90, 0xF0, 0x10, 0xF0),
            array(0xF0, 0x90, 0xF0, 0x90, 0x90),
            array(0xE0, 0x90, 0xE0, 0x90, 0xE0),
            array(0xF0, 0x80, 0x80, 0x80, 0xF0),
            array(0xE0, 0x90, 0x90, 0x90, 0xE0),
            array(0xF0, 0x80, 0xF0, 0x80, 0xF0),
            array(0xF0, 0x80, 0xF0, 0x80, 0x80))

    var running : Boolean = true

    {
        pc = 0x200
        initMemory()
        loadProgram()
        if (debug) printMemory()
        while (running) {
            val instruction: Short = fetch()
            if (debug) println(java.lang.String.format("mem[%04x]: %04x", pc, instruction))
            interpret(instruction)
            pc = (pc + 2).toShort()
            if (pc >= 4095) running = false
        }
    }

    fun initMemory() {
        initializeSprites()
    }

    fun loadProgram() {
        val file : File = File(filename)
        var marker = 0x200
        for (byte in file.readBytes()) {
            mem[marker++] = byte
        }
    }

    fun initializeSprites() {
        for (i in sprites.indices) {
            for (j in sprites[i].indices) {
                mem[(i * 5) + j] = sprites[i][j].toByte()
            }
        }
    }

    fun fetch() : Short {
        return ((mem[pc.toInt()].toInt() shl 8) or (mem[pc + 1].toInt() and 0xFF)).toShort()
    }

    fun interpret(inst : Short) {

        val inst = inst.toInt() and 0xFFFF
        val op = (inst and 0xF000) shr 12
        val nnn : Short  = (inst and 0x0FFF).toShort()
        val n    = (inst and 0xF)
        val x    = (inst and 0xF00) shr 8
        val y    = (inst and 0xF0) shr 4
        val kk   = (inst and 0xFF).toByte()

        when (op) {
            0 -> when (inst and 0x000F) {
                0    -> clearScreen()
                0xE  -> ret()
                else -> println("SYS - unused instruction")
            }
            1 -> jump(nnn)
            2 -> call(nnn)
            3 -> if (reg[x] == kk) pc = (pc + 2).toShort()
            4 -> if (reg[x] != kk) pc = (pc + 2).toShort()
            5 -> if (reg[x] == reg[y]) pc = (pc + 2).toShort()
            6 -> reg[x] = kk
            7 -> reg[x] = ((reg[x].toInt() and 0xFFFF) + kk).toByte()
            8 -> {
                if (debug) println("8xyn call")
                when (n) {
                    0 -> reg[x] = reg[y]
                    1 -> reg[x] = ((reg[x].toInt() and 0xFFFF) or reg[y].toInt() and 0xFFFF).toByte()
                    2 -> reg[x] = ((reg[x].toInt() and 0xFFFF) and reg[y].toInt() and 0xFFFF).toByte()
                    3 -> reg[x] = ((reg[x].toInt() and 0xFFFF) xor reg[y].toInt() and 0xFFFF).toByte()
                    4 -> {
                        reg[15] = if (reg[x] + reg[y] > 255) 1 else 0
                        reg[x] = (reg[x] + reg[y]).toByte()
                    }
                    5 -> {
                        reg[15] = if (reg[x] > reg[y]) 1 else 0
                        reg[x] = (reg[x] - reg[y]).toByte()
                    }
                    6 -> {
                        reg[15] = ((reg[x].toInt() and 0xFFFF) and 1).toByte()
                        reg[x] = ((reg[x].toInt() and 0xFFFF) shr 1).toByte()
                    }
                    7 -> {
                        reg[15] = if ((reg[y] > reg[x])) 1 else 0
                        reg[x] = (reg[y] - reg[x]).toByte()
                    }
                    0xE -> {
                        reg[15] = (((reg[x].toInt() and 0xFFFF) shl 7) and 1).toByte()
                        reg[x] = ((reg[x].toInt() and 0xFFFF) shl 1).toByte()
                    }
                }
            }
            9 -> {
                when (n) {
                    0 -> (if (reg[x] != reg[y]) pc = (pc + 2).toShort())
                    else -> println("Invalid 9xyn call")
                }
            }
            0xD -> {
                if (debug) println("DRW")
                for (i in vI..(vI + n)) {
                    for (j in 0..7) {
                        if (((mem[i].toInt() and (1 shl j)) shr j) == 1) {
                            screen.setPixel(reg[x].toInt() + (7 - j), reg[y].toInt() + (i - vI), true)
                        } else {
                            screen.setPixel(reg[x].toInt() + (7 - j), reg[y].toInt() + (i - vI), false)
                        }
                    }
                }
            }
        }
    }

    fun clearScreen() {
        if (debug) println("CLS")
        screen.clearScreen()
    }

    fun ret() {
        if (debug) println("RET")
        pc = (stack[sp.toInt()] - 2).toShort()
        sp = (sp - 1).toByte()
    }

    fun jump (addr : Short) {
        if (debug) println("JMP(${java.lang.String.format("%03x", addr)})")
        pc = (addr - 2).toShort()
    }

    fun call (addr : Short) {
        if (debug) println("CALL(${java.lang.String.format("%03x", addr)}) - sp: ${sp}}")
        if (sp >= 15) {
            throw IndexOutOfBoundsException("Stack overflow lol 16 frames too much")
        } else {
            stack.plus(pc)
            sp = (sp + 1).toByte()
            pc = (addr - 2).toShort()
        }
    }

    fun printMemory() {
        if (debug) println("--- Start of Chip8 RAM ---")
        for (i in 0..0x200 - 1)
            if (i % 4 == 0)
                print(java.lang.String.format("%02x%02x %02x%02x\n", mem[i], mem[i + 1], mem[i + 2], mem[i + 3]))
        if (debug) println("--- Address 0x200 ---")
        for (i in 0x200..0x600 - 1)
            if (i % 4 == 0)
                print(java.lang.String.format("%04x: %02x%02x %02x%02x\n", i, mem[i], mem[i + 1], mem[i + 2], mem[i + 3]))

    }

}

class Chip8Screen() : Frame() {

    final val debug : Boolean = false
    var pixels = Array(64, { i -> Array(32, { j -> false } ) });

    {
        setSize(640, 320)
        setBackground(Color.black)
        setVisible(true)
        addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                dispose()
                System.exit(0)
            }
        })
        val runThread : Thread = Thread(object : Thread() {
            override fun run() {
                while (true) {
                    repaint()
                    Thread.sleep(1000)
                }
            }
        })
        runThread.start()
    }

    fun clearScreen() {
        for (i in pixels.indices) {
            for (j in pixels[i].indices) {
                pixels[i][j] = false
            }
        }
    }

    fun setPixel(x : Int, y: Int, on : Boolean) {
        if (debug) println("set pixel ${x}, ${y}: ${on}")
        this.pixels[x][y] = on
    }

    override fun paint(g: Graphics) {
        super.paint(g)
        if (debug) println("call to paint")
        for (i in pixels.indices) {
            for (j in pixels[i].indices) {
                if (pixels[i][j]) {
                    g.setColor(Color.green)
                    g.fillRect(i * 10, j * 10, 10, 10)
                } else {
                    g.setColor(Color.black)
                    g.fillRect(i * 10, j * 10, 10, 10)
                }
            }
        }
    }
}
