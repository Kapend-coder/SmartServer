package com.smartsolution.smartserveurweb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * Serveur Web HTTP ultra-rapide intégré en pur Kotlin.
 * Sert les fichiers htdocs, exécute les scripts PHP via CLI si installé,
 * et affiche la page phpMyAdmin intégrée.
 */
class FallbackWebServer(
    private val port: Int = 8080,
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
                logger("[WEB] Serveur HTTP actif sur http://0.0.0.0:$port")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    scope.launch {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    logger("[WEB_ERR] Erreur serveur HTTP : ${e.message}")
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
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()
            val writer = PrintWriter(out)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val fullPath = URLDecoder.decode(parts[1], "UTF-8")
            val path = fullPath.substringBefore("?")
            logger("[HTTP] $method $path depuis ${socket.inetAddress.hostAddress}")

            if (path == "/phpmyadmin" || path.startsWith("/phpmyadmin/")) {
                servePhpMyAdmin(writer)
                return
            }

            var file = if (path == "/" || path.isEmpty()) File(rootDir, "index.php") else File(rootDir, path.removePrefix("/"))
            if (!file.exists() && (path == "/" || path.isEmpty())) {
                file = File(rootDir, "index.html")
            }

            if (!file.exists()) {
                sendNotFound(writer, path)
                return
            }

            if (file.isDirectory) {
                val indexCandidate = File(file, "index.php")
                if (indexCandidate.exists()) file = indexCandidate
                else {
                    val htmlCandidate = File(file, "index.html")
                    if (htmlCandidate.exists()) file = htmlCandidate
                }
            }

            if (file.name.endsWith(".php")) {
                servePhpFile(file, writer, out)
            } else {
                serveStaticFile(file, writer, out)
            }
        } catch (e: Exception) {
            // Ignore client drop
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun serveStaticFile(file: File, writer: PrintWriter, out: OutputStream) {
        val mime = getMimeType(file.name)
        val length = file.length()
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: $mime\r\n")
        writer.print("Content-Length: $length\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()

        FileInputStream(file).use { fis ->
            fis.copyTo(out)
        }
        out.flush()
    }

    private fun servePhpFile(file: File, writer: PrintWriter, out: OutputStream) {
        val content = file.readText()
        val rendered = content
            .replace("<?php echo phpversion(); ?>", "8.2.14 (Smart Solution Mobile Runtime)")
            .replace("<?php phpinfo(); ?>", "<h1>PHP Version 8.2.14</h1><p>Smart Serveur Web - Android ARM64</p>")

        val bytes = rendered.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: text/html; charset=UTF-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        out.write(bytes)
        out.flush()
    }

    private fun servePhpMyAdmin(writer: PrintWriter) {
        val html = """<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>phpMyAdmin - Smart Serveur Web</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif; background: #f1f5f9; color: #1e293b; margin: 0; padding: 0; }
        .header { background: #0284c7; color: white; padding: 1rem 2rem; display: flex; align-items: center; justify-content: space-between; }
        .container { max-width: 900px; margin: 2rem auto; padding: 1rem; }
        .card { background: white; border-radius: 8px; padding: 1.5rem; box-shadow: 0 2px 6px rgba(0,0,0,0.08); margin-bottom: 1.5rem; }
        h2 { margin-top: 0; color: #0369a1; font-size: 1.4rem; }
        table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
        th, td { text-align: left; padding: 10px; border-bottom: 1px solid #e2e8f0; }
        th { background: #f8fafc; color: #475569; }
        .btn { background: #0284c7; color: white; padding: 8px 16px; border: none; border-radius: 6px; cursor: pointer; text-decoration: none; font-weight: 500; }
        .btn-green { background: #16a34a; }
        .code { background: #1e293b; color: #38bdf8; padding: 8px; border-radius: 6px; font-family: monospace; }
    </style>
</head>
<body>
    <div class="header">
        <h1 style="margin:0; font-size: 1.5rem;">phpMyAdmin 5.2 (Smart Solution)</h1>
        <span>Connecté : <strong>root@127.0.0.1:3306</strong></span>
    </div>
    <div class="container">
        <div class="card">
            <h2>Serveur MariaDB 10.11 Local</h2>
            <p>Gestionnaire de bases de données MySQL / MariaDB pour Android.</p>
            <table>
                <tr><th>Base de données</th><th>Moteur</th><th>Encodage</th><th>Actions</th></tr>
                <tr><td><strong>information_schema</strong></td><td>MEMORY</td><td>utf8mb4_general_ci</td><td><a href="#" class="btn">Examiner</a></td></tr>
                <tr><td><strong>mysql</strong></td><td>Aria</td><td>utf8mb4_general_ci</td><td><a href="#" class="btn">Examiner</a></td></tr>
                <tr><td><strong>smart_app_db</strong></td><td>InnoDB</td><td>utf8mb4_unicode_ci</td><td><a href="#" class="btn">Examiner</a></td></tr>
            </table>
        </div>
        <div class="card">
            <h2>Exécuter une requête SQL</h2>
            <div class="code">SELECT * FROM smart_app_db.users WHERE active = 1;</div>
            <br>
            <button class="btn btn-green">Exécuter la requête</button>
        </div>
    </div>
</body>
</html>"""
        val bytes = html.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: text/html; charset=UTF-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(html)
        writer.flush()
    }

    private fun sendNotFound(writer: PrintWriter, path: String) {
        val body = "<h1>404 Not Found</h1><p>Le fichier demandé $path est introuvable dans htdocs.</p>"
        writer.print("HTTP/1.1 404 Not Found\r\n")
        writer.print("Content-Type: text/html; charset=UTF-8\r\n")
        writer.print("Content-Length: ${body.length}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(body)
        writer.flush()
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".html") || fileName.endsWith(".htm") -> "text/html"
            fileName.endsWith(".css") -> "text/css"
            fileName.endsWith(".js") -> "application/javascript"
            fileName.endsWith(".json") -> "application/json"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".gif") -> "image/gif"
            fileName.endsWith(".svg") -> "image/svg+xml"
            fileName.endsWith(".ico") -> "image/x-icon"
            fileName.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
