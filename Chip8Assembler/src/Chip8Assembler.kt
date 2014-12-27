import java.io.File
import java.util.Scanner
import java.util.LinkedList
import java.util.Stack
import java.util.ArrayList

fun main(args: Array<String>) {

    val debug : Boolean = true
    val file : File = File(args[0])
    val scanner : Scanner = Scanner(file)
    val outFile : File = File("out.c8")
    if (outFile.isFile()) outFile.delete()
    outFile.createNewFile()
    var fullInst : Int
    var vx : Int
    var vy : Int
    var byte : Int

    while (scanner.hasNext()) {
        var token : String = scanner.next();
        if (debug) println("token: ${token}")
        when (token) {
            "CLS" -> {
                outFile.appendBytes(splitInstruction(0x00E0))
            }
            "RET" -> {
                outFile.appendBytes(splitInstruction(0x00EE))
            }
            "JP" -> {
                fullInst = 0x1000 or getAddress(scanner)
                println(java.lang.String.format("fullInst: %04x", fullInst))
                outFile.appendBytes(splitInstruction(fullInst))
            }
            "CALL" -> {
                fullInst = 0x2000 or getAddress(scanner)
                println(java.lang.String.format("fullInst: %04x", fullInst))
                outFile.appendBytes(splitInstruction(fullInst))
            }
            "SE" -> {
                token = scanner.next()
                if (tokenIsRegister(token)) {
                    vx = getInt(token)
                    println("SE: got vx: ${vx}")
                } else {
                    throw UnexpectedTokenException("expected V register number, got: ${token}")
                }
                token = scanner.next()
                if (tokenIsRegister(token)) {
                    vy = getInt(token)
                    println("SE: got vy: ${vy}")
                    fullInst = (0x5000 or (vx shl 8)) or (vy shl 4)
                } else {
                    byte = Integer.parseInt(token, 16)
                    println("SE: got byte: ${byte}")
                    fullInst = (0x3000 or (vx shl 8)) or byte
                }
                outFile.appendBytes(splitInstruction(fullInst))
            }
            "SNE" -> {
                token = scanner.next()
                if (tokenIsRegister(token)) {
                    vx = getInt(token)
                } else {
                    throw UnexpectedTokenException("expected V register number, got: ${token}")
                }
                token = scanner.next()
                if (tokenIsRegister(token)) {
                    vy = getInt(token)
                    println("SNE: got vy: ${vy}")
                    fullInst = (0x4000 or (vx shl 8)) or (vy shl 4)
                } else {
                    byte = Integer.parseInt(token, 16)
                    println("SE: got byte: ${byte}")
                    fullInst = (0x9000 or (vx shl 8)) or byte
                }
                outFile.appendBytes(splitInstruction(fullInst))
            }
            "LD" -> {
                token = scanner.next()
                when (token[0]) {
                    'I' -> fullInst = 0xA000 or getAddress(scanner)
                    'D' -> fullInst = 0xF015 or (getInt(scanner.next()) shl 8)
                    'S' -> fullInst = 0xF018 or (getInt(scanner.next()) shl 8)
                    'V' -> {
                        vx = getInt(token)
                        token = scanner.next()
                        when (token[0]) {
                            'V' -> {
                                vy = getInt(token)
                                fullInst = (0x8000 or (vx shl 8)) or (vy shl 4)
                            }
                            'D' -> {
                                fullInst = 0xF007 or (vx shl 8)
                            }
                            'K' -> {
                                fullInst = 0xF00A or (vx shl 8)
                            }
                            '[' -> {
                                fullInst = 0xF065 or (vx shl 8)
                            }
                            else -> {
                                byte = Integer.parseInt(token, 16)
                                fullInst = (0x6000 or (vx shl 8)) or byte
                            }
                        }
                    }
                    'F' -> fullInst = 0xF029 or (getInt(scanner.next()) shl 8)
                    'B' -> fullInst = 0xF033 or (getInt(scanner.next()) shl 8)
                    '[' -> fullInst = 0xF055 or (getInt(scanner.next()) shl 8)
                    else -> throw UnexpectedTokenException("expected one of ['I', 'D', 'S', 'V', 'F', 'B', '[I]'], got: ${token}")
                }
                outFile.appendBytes(splitInstruction(fullInst))
            }
            "ADD" -> {

            }
            "OR" -> {

            }
            "AND" -> {

            }
            "XOR" -> {

            }
            "SUB" -> {

            }
            "SHR" -> {

            }
        }
    }
}

fun getAddress(scanner : Scanner) : Int {
    var addr : Int
    if (scanner.hasNextInt(16)) {
        addr = scanner.nextInt(16)
    } else {
        throw UnexpectedArgumentException("expected addr, got: ${scanner.next()}")
    }
    if (addr > 0xFFF) {
        throw AddressOutOfRangeException("address must be 12-bit value, got: " +
                "${java.lang.String.format("%04x", addr)})}")
    }
    return addr
}

fun tokenIsRegister(token : String) : Boolean {
    return (token.startsWith('V') || token.startsWith('v'))
}

fun splitInstruction(fullInst: Int): ByteArray {
    var splitInst : ByteArray = ByteArray(2)
    splitInst[0] = ((fullInst shr 8) and 0xFF).toByte()
    splitInst[1] = (fullInst and 0xFF).toByte()
    return splitInst
}

class UnexpectedArgumentException(s: String) : Exception(s) {}
class AddressOutOfRangeException(s: String) : Exception(s) {}
class UnexpectedTokenException(s: String) : Exception(s) {}

fun getInt(token: String): Int {
    println("trying to get int out of ${token}")
    return Integer.parseInt(token.replaceAll("[\\D]", ""))
}
