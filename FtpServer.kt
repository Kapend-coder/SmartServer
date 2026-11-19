package com.smartsolution.smartserveurweb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * Serveur FTP natif en pur Kotlin.
 * Fonctionne à 100% sans root et sans dépendance externe sur Android.
 */
class FtpServer(
    private val port: Int = 2121,
    private val user: String = "serveur",
    private val pass: String = "serveur123",
    private val rootDir: File,
    private val logger: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun start() {
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                logger("[FTP] Serveur en écoute sur 0.0.0.0:$port (identifiants: $user / $pass)")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    scope.launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    logger("[FTP_ERR] Erreur socket FTP : ${e.message}")
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        job.cancel()
    }

    private fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress.hostAddress
        logger("[FTP] Nouvelle connexion client : $clientIp")
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            writer.println("220 Smart Solution FTP Server Ready")

            var authenticated = false
            var currentDir = rootDir
            var currentUser = ""
            var passiveServerSocket: ServerSocket? = null

            while (isRunning && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val parts = line.trim().split(" ", limit = 2)
                val cmd = parts[0].uppercase()
                val arg = if (parts.size > 1) parts[1] else ""

                when (cmd) {
                    "USER" -> {
                        currentUser = arg
                        writer.println("331 Mot de passe requis pour $arg")
                    }
                    "PASS" -> {
                        if (currentUser == user && arg == pass) {
                            authenticated = true
                            writer.println("230 Utilisateur connecté avec succès")
                            logger("[FTP] Client $clientIp authentifié en tant que $currentUser")
                        } else {
                            writer.println("530 Identifiants incorrects")
                        }
                    }
                    "SYST" -> writer.println("215 UNIX Type: L8")
                    "FEAT" -> {
                        writer.println("211-Features:")
                        writer.println(" UTF8")
                        writer.println("211 End")
                    }
                    "PWD" -> {
                        val relPath = currentDir.absolutePath.removePrefix(rootDir.absolutePath).ifEmpty { "/" }
                        writer.println("257 "$relPath" est le répertoire courant")
                    }
                    "TYPE" -> writer.println("200 Type défini sur $arg")
                    "PASV" -> {
                        passiveServerSocket?.close()
                        passiveServerSocket = ServerSocket(0)
                        val pasvPort = passiveServerSocket!!.localPort
                        val p1 = pasvPort / 256
                        val p2 = pasvPort % 256
                        val ipParts = socket.localAddress.hostAddress.replace('.', ',')
                        writer.println("227 Entering Passive Mode ($ipParts,$p1,$p2)")
                    }
                    "LIST" -> {
                        writer.println("150 Début du transfert de la liste des fichiers")
                        val dataSocket = passiveServerSocket?.accept()
                        dataSocket?.use { ds ->
                            val dataWriter = PrintWriter(ds.getOutputStream(), true)
                            val files = currentDir.listFiles() ?: emptyArray()
                            for (file in files) {
                                val type = if (file.isDirectory) "d" else "-"
                                val perm = "rwxr-xr-x"
                                val size = file.length()
                                val name = file.name
                                dataWriter.println("$type$perm 1 serveur serveur $size Jan 01 00:00 $name")
                            }
                        }
                        passiveServerSocket?.close()
                        passiveServerSocket = null
                        writer.println("226 Transfert terminé")
                    }
                    "CWD" -> {
                        val target = if (arg.startsWith("/")) File(rootDir, arg.removePrefix("/")) else File(currentDir, arg)
                        if (target.exists() && target.isDirectory) {
                            currentDir = target
                            writer.println("250 Répertoire changé vers ${currentDir.name}")
                        } else {
                            writer.println("550 Répertoire introuvable")
                        }
                    }
                    "CDUP" -> {
                        if (currentDir.absolutePath != rootDir.absolutePath) {
                            currentDir = currentDir.parentFile ?: rootDir
                        }
                        writer.println("200 Répertoire parent atteint")
                    }
                    "QUIT" -> {
                        writer.println("221 Au revoir")
                        break
                    }
                    "NOOP" -> writer.println("200 OK")
                    else -> writer.println("502 Commande non implémentée")
                }
            }
        } catch (e: Exception) {
            logger("[FTP] Fin de session pour $clientIp")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
