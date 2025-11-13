package com.btcemais.vpnmonitor

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    // Declaração de todas as Views
    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
    private lateinit var tvNetwork: TextView
    private lateinit var tvDnsLeak: TextView
    private lateinit var tvWebRtc: TextView
    private lateinit var tvMultipleIps: TextView
    private lateinit var tvGeolocation: TextView
    private lateinit var tvTimeZone: TextView
    private lateinit var tvVpnDetection: TextView

    // Configuração do escopo de corrotinas
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Companion object para constantes
    private companion object {
        const val TAG = "VPNMonitor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "Activity criada - onCreate()")

        // Inicializar todas as views
        initViews()

        // Configurar listeners dos botões
        setupClickListeners()

        // Log inicial do sistema
        logSystemInfo()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity retomada - onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity pausada - onPause()")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar todas as corrotinas quando a activity for destruída
        coroutineScope.cancel()
        Log.d(TAG, "Activity destruída - onDestroy()")
    }

    /**
     * Inicializa todas as views do layout
     */
    private fun initViews() {
        Log.d(TAG, "Inicializando views...")

        tvStatus = findViewById(R.id.tv_status)
        tvIp = findViewById(R.id.tv_ip)
        tvNetwork = findViewById(R.id.tv_network)
        tvDnsLeak = findViewById(R.id.tv_dns_leak)
        tvWebRtc = findViewById(R.id.tv_web_rtc)
        tvMultipleIps = findViewById(R.id.tv_multiple_ips)
        tvGeolocation = findViewById(R.id.tv_geolocation)
        tvTimeZone = findViewById(R.id.tv_timezone)
        tvVpnDetection = findViewById(R.id.tv_vpn_detection)

        // Configurar textos iniciais
        tvStatus.text = "Status VPN: Não verificado"
        tvIp.text = "IP Público: Não detectado"
        tvNetwork.text = "Rede: Não verificada"
        tvDnsLeak.text = "Vazamento DNS: Não verificado"
        tvWebRtc.text = "WebRTC: Não verificado"
        tvMultipleIps.text = "IPs Múltiplos: Não verificado"
        tvGeolocation.text = "Geolocalização: Não verificada"
        tvTimeZone.text = "Fuso Horário: Não verificado"
        tvVpnDetection.text = "Detecção VPN: Não verificada"

        Log.d(TAG, "Views inicializadas com sucesso")
    }

    /**
     * Configura os listeners dos botões
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Configurando listeners dos botões...")

        val btnCheck = findViewById<Button>(R.id.btn_check)
        val btnAdvanced = findViewById<Button>(R.id.btn_advanced)

        btnCheck.setOnClickListener {
            Log.d(TAG, "Botão 'Verificação Básica' clicado")
            checkVPNStatus()
        }

        btnAdvanced.setOnClickListener {
            Log.d(TAG, "Botão 'Testes Avançados' clicado")
            runAdvancedTests()
        }

        Log.d(TAG, "Listeners configurados com sucesso")
    }

    /**
     * Exibe uma mensagem para o usuário usando Toast
     */
    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Mensagem exibida para usuário: $message")
    }

    /**
     * Trata diferentes tipos de erro de rede e exibe mensagens apropriadas
     */
    private fun handleNetworkError(error: Exception) {
        Log.e(TAG, "Tratando erro de rede: ${error.javaClass.simpleName} - ${error.message}")

        when (error) {
            is java.net.SocketTimeoutException -> {
                Log.w(TAG, "Timeout na conexão de rede")
                showMessage("Timeout - Verifique sua conexão com a internet")
            }
            is java.net.UnknownHostException -> {
                Log.w(TAG, "Host desconhecido - sem conexão com internet")
                showMessage("Sem conexão com a internet")
            }
            is java.io.IOException -> {
                Log.w(TAG, "Erro de IO na rede: ${error.message}")
                showMessage("Erro de conexão: ${error.message ?: "Erro desconhecido"}")
            }
            is kotlinx.coroutines.TimeoutCancellationException -> {
                Log.w(TAG, "Operação de corrotina muito lenta")
                showMessage("Operação muito lenta - tente novamente")
            }
            is SecurityException -> {
                Log.w(TAG, "Problema de permissão: ${error.message}")
                showMessage("Erro de permissão - verifique as permissões do app")
            }
            else -> {
                Log.e(TAG, "Erro genérico de rede: ${error.message}")
                showMessage("Erro: ${error.message?.take(50) ?: "Erro desconhecido"}")
            }
        }
    }

    /**
     * Registra informações do sistema e rede para debugging
     */
    private fun logSystemInfo() {
        Log.d(TAG, "=== INFORMACOES DO SISTEMA ===")
        Log.d(TAG, "Timestamp: ${System.currentTimeMillis()}")
        Log.d(TAG, "Dispositivo: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        Log.d(TAG, "Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        Log.d(TAG, "App: VPN Monitor v1.0")
        Log.d(TAG, "Package: com.btcemais.vpnmonitor")
    }

    /**
     * Registra informações específicas da rede para uma operação
     */
    private fun logNetworkInfo(context: String = "Verificação") {
        Log.d(TAG, "=== $context - INICIO ===")
        Log.d(TAG, "Contexto: $context")
        Log.d(TAG, "Timestamp: ${System.currentTimeMillis()}")

        try {
            val connectivityManager = getSystemService(ConnectivityManager::class.java)
            val network = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(network)

            Log.d(TAG, "Rede ativa: ${network != null}")
            Log.d(TAG, "VPN ativa: ${caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true}")
            Log.d(TAG, "Internet disponível: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true}")
            Log.d(TAG, "Rede validada: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true}")

            // Log do tipo de rede
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> Log.d(TAG, "Tipo: WiFi")
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> Log.d(TAG, "Tipo: Celular")
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> Log.d(TAG, "Tipo: Ethernet")
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> Log.d(TAG, "Tipo: VPN")
                else -> Log.d(TAG, "Tipo: Desconhecido")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao obter informações de rede: ${e.message}")
        }
        Log.d(TAG, "=== $context - FIM ===")
    }

    /**
     * Mostra/oculta o estado de carregamento e controla botões
     */
    private fun showLoading(loading: Boolean) {
        val btnCheck = findViewById<Button>(R.id.btn_check)
        val btnAdvanced = findViewById<Button>(R.id.btn_advanced)

        btnCheck.isEnabled = !loading
        btnAdvanced.isEnabled = !loading

        if (loading) {
            Log.d(TAG, "Modo carregamento: ATIVADO")
            btnCheck.text = "Verificando..."
            btnAdvanced.text = "Verificando..."
        } else {
            Log.d(TAG, "Modo carregamento: DESATIVADO")
            btnCheck.text = "Verificação Básica"
            btnAdvanced.text = "Testes Avançados"
        }
    }

    /**
     * Executa a verificação básica do status da VPN
     */
    private fun checkVPNStatus() {
        Log.d(TAG, "Iniciando verificação básica do VPN...")
        logNetworkInfo("Verificação Básica")

        coroutineScope.launch {
            showLoading(true)
            try {
                // Executar verificações em sequência
                updateNetworkInfo()
                val publicIp = fetchPublicIP()
                tvIp.text = "IP Público: $publicIp"

                // Executar verificações em paralelo
                val deferredTasks = listOf(
                    async { checkMultipleIPServices() },
                    async { checkDNSLeak() },
                    async { checkLocalIP() }
                )
                deferredTasks.awaitAll()

                Log.d(TAG, "Verificação básica concluída com sucesso")
                showMessage("Verificação básica concluída!")

            } catch (e: Exception) {
                Log.e(TAG, "Erro na verificação básica: ${e.message}", e)
                handleNetworkError(e)
                tvIp.text = "IP Público: Erro - ${e.message}"
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Executa testes avançados de privacidade
     */
    private fun runAdvancedTests() {
        Log.d(TAG, "Iniciando testes avançados...")
        logNetworkInfo("Testes Avançados")

        coroutineScope.launch {
            showLoading(true)
            try {
                // Executar todos os testes avançados em paralelo
                val advancedTasks = listOf(
                    async { checkWebRTCLeak() },
                    async { checkGeolocation() },
                    async { checkTimeZone() },
                    async { checkVPNDetection() }
                )
                advancedTasks.awaitAll()

                Log.d(TAG, "Testes avançados concluídos com sucesso")
                showMessage("Testes avançados concluídos!")

            } catch (e: Exception) {
                Log.e(TAG, "Erro nos testes avançados: ${e.message}", e)
                handleNetworkError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Atualiza informações básicas da rede
     */
    private fun updateNetworkInfo() {
        Log.d(TAG, "Atualizando informações de rede...")

        try {
            val connectivityManager = getSystemService(ConnectivityManager::class.java)
            val network = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(network)

            // Determinar status da VPN
            val vpnStatus = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "✅ Conectado"
                else -> "❌ Desconectado"
            }

            tvStatus.text = "Status VPN: $vpnStatus"

            // Determinar tipo de rede e informações adicionais
            val networkInfo = StringBuilder()

            // Tipo de rede
            val networkType = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Desconhecido"
            }
            networkInfo.append("Tipo de Rede: $networkType\n")

            // Capacidades da rede
            caps?.let {
                if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    networkInfo.append("Internet: ✅ Disponível\n")
                } else {
                    networkInfo.append("Internet: ❌ Indisponível\n")
                }

                if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    networkInfo.append("Conexão: ✅ Validada\n")
                } else {
                    networkInfo.append("Conexão: ⚠️ Não validada\n")
                }

                if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    networkInfo.append("Dados: ✅ Ilimitados\n")
                } else {
                    networkInfo.append("Dados: ⚠️ Limitados\n")
                }
            }

            tvNetwork.text = networkInfo.toString()
            Log.d(TAG, "Informações de rede atualizadas: VPN=$vpnStatus, Tipo=$networkType")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter informações de rede: ${e.message}", e)
            tvStatus.text = "Status VPN: Erro"
            tvNetwork.text = "Rede: Erro na verificação\n${e.message}"
        }
    }

    /**
     * Obtém o IP público do dispositivo
     */
    private suspend fun fetchPublicIP(): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando obtenção do IP público...")

        try {
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"

            Log.d(TAG, "Conectando ao api.ipify.org...")

            val ip = connection.inputStream.bufferedReader().use { it.readText().trim() }

            if (ip.isNotBlank() && ip.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))) {
                Log.d(TAG, "IP público obtido com sucesso: $ip")
                return@withContext ip
            } else {
                Log.w(TAG, "Resposta inválida do serviço de IP: $ip")
                return@withContext "Erro: Resposta inválida"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter IP público: ${e.message}", e)
            return@withContext "Erro: ${e.message ?: "Falha na conexão"}"
        }
    }

    /**
     * Verifica possíveis vazamentos de DNS
     */
    private suspend fun checkDNSLeak() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando verificação de vazamento DNS...")

        try {
            val dnsServers = NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress }
                .map { it.hostAddress to it.hostName }
                .distinct()

            Log.d(TAG, "Encontrados ${dnsServers.size} servidores DNS")

            val dnsInfo = StringBuilder()
            dnsInfo.append("Servidores DNS encontrados: ${dnsServers.size}\n")

            dnsServers.take(5).forEach { (ip, host) ->
                dnsInfo.append("$ip ($host)\n")
            }

            // Heurística: Mais de 4 servidores pode indicar vazamento
            val hasPotentialLeak = dnsServers.size > 4
            val status = if (hasPotentialLeak) "⚠️ Possível vazamento" else "✅ Provavelmente seguro"

            withContext(Dispatchers.Main) {
                tvDnsLeak.text = "DNS: $status\n$dnsInfo"
            }

            Log.d(TAG, "Verificação DNS concluída: $status (${dnsServers.size} servidores)")

        } catch (e: Exception) {
            Log.e(TAG, "Erro na verificação DNS: ${e.message}", e)
            withContext(Dispatchers.Main) {
                tvDnsLeak.text = "DNS: Erro na verificação\n${e.message}"
            }
        }
    }

    /**
     * Verifica consistência do IP em múltiplos serviços
     */
    private suspend fun checkMultipleIPServices() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando verificação de múltiplos serviços de IP...")

        val services = listOf(
            "https://api.ipify.org" to "IPify",
            "https://icanhazip.com" to "ICanHazIP",
            "https://checkip.amazonaws.com" to "AWS",
            "https://ifconfig.me/ip" to "IfConfig"
        )

        val results = mutableListOf<Pair<String, String>>()

        services.forEach { (service, name) ->
            try {
                Log.d(TAG, "Testando serviço: $name ($service)")

                withTimeout(5000) {
                    val ip = URL(service).openConnection().apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                    }.getInputStream().bufferedReader().use { it.readText().trim() }

                    if (ip.isNotBlank() && ip.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))) {
                        results.add(name to ip)
                        Log.d(TAG, "Serviço $name retornou: $ip")
                    } else {
                        Log.w(TAG, "Serviço $name retornou resposta inválida: $ip")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Serviço $name falhou: ${e.message}")
            }
        }

        val ips = results.map { it.second }.distinct()
        val ipCount = ips.size

        // Determinar status baseado na consistência
        val status = when {
            ipCount == 0 -> "❌ Todos falharam"
            ipCount == 1 -> "✅ Consistente"
            ipCount == 2 -> "⚠️ Leve inconsistência"
            else -> "❌ Inconsistente"
        }

        val resultText = StringBuilder()
        resultText.append("$status ($ipCount IPs diferentes)\n")
        resultText.append("Serviços respondidos: ${results.size}/${services.size}\n")
        results.take(3).forEach { (name, ip) ->
            resultText.append("$name: ${ip.take(15)}...\n")
        }

        withContext(Dispatchers.Main) {
            tvMultipleIps.text = resultText.toString()
        }

        Log.d(TAG, "Verificação múltipla de IPs concluída: $status")
    }

    /**
     * Verifica IPs locais da rede
     */
    private fun checkLocalIP() {
        Log.d(TAG, "Iniciando verificação de IPs locais...")

        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            val localIPs = StringBuilder("IPs Locais:\n")
            var interfaceCount = 0
            var ipCount = 0

            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress.contains('.')) {
                        localIPs.append("${networkInterface.displayName}: ${address.hostAddress}\n")
                        ipCount++
                    }
                }
                interfaceCount++
            }

            if (ipCount == 0) {
                localIPs.append("Nenhum IP local encontrado")
            }

            tvNetwork.text = "${tvNetwork.text}\n$localIPs"
            Log.d(TAG, "IPs locais coletados: $ipCount IPs em $interfaceCount interfaces")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter IPs locais: ${e.message}")
            tvNetwork.text = "${tvNetwork.text}\nErro ao obter IPs locais"
        }
    }

    /**
     * Verifica geolocalização baseada no IP
     */
    private suspend fun checkGeolocation() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando verificação de geolocalização...")

        try {
            val ip = fetchPublicIP()
            if (!ip.startsWith("Erro") && ip.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))) {
                Log.d(TAG, "Consultando geolocalização para IP: $ip")

                val geoInfo = withTimeout(10000) {
                    URL("http://ip-api.com/json/$ip").openStream().bufferedReader().use { it.readText() }
                }

                // Parse básico do JSON retornado
                val country = geoInfo.substringAfter("\"country\":\"").substringBefore("\"")
                val city = geoInfo.substringAfter("\"city\":\"").substringBefore("\"")
                val isp = geoInfo.substringAfter("\"isp\":\"").substringBefore("\"")
                val countryCode = geoInfo.substringAfter("\"countryCode\":\"").substringBefore("\"")

                val locationText = StringBuilder()
                locationText.append("📍 Geolocalização:\n")
                locationText.append("País: $country ($countryCode)\n")
                locationText.append("Cidade: $city\n")
                locationText.append("ISP: $isp\n")
                locationText.append("IP: $ip")

                withContext(Dispatchers.Main) {
                    tvGeolocation.text = locationText.toString()
                }

                Log.d(TAG, "Geolocalização obtida: $country, $city, $isp")

            } else {
                Log.w(TAG, "IP inválido para geolocalização: $ip")
                withContext(Dispatchers.Main) {
                    tvGeolocation.text = "📍 Geolocalização: IP inválido\n$ip"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na geolocalização: ${e.message}", e)
            withContext(Dispatchers.Main) {
                tvGeolocation.text = "📍 Geolocalização: Erro\n${e.message}"
            }
        }
    }

    /**
     * Verifica informações de fuso horário e localidade
     */
    private fun checkTimeZone() {
        Log.d(TAG, "Iniciando verificação de fuso horário...")

        try {
            val timeZone = java.util.TimeZone.getDefault()
            val locale = java.util.Locale.getDefault()
            val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", locale)
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", locale)
            val now = java.util.Date()

            val timeInfo = StringBuilder()
            timeInfo.append("⏰ Informações de Tempo:\n")
            timeInfo.append("Fuso: ${timeZone.id}\n")
            timeInfo.append("Horário: ${timeFormat.format(now)}\n")
            timeInfo.append("Data: ${dateFormat.format(now)}\n")
            timeInfo.append("Idioma: ${locale.displayLanguage}\n")
            timeInfo.append("País: ${locale.displayCountry}\n")
            timeInfo.append("Offset UTC: ${timeZone.getOffset(now.time) / (1000 * 60 * 60)}h")

            tvTimeZone.text = timeInfo.toString()
            Log.d(TAG, "Informações de tempo coletadas: ${timeZone.id}, ${locale.displayCountry}")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter informações de tempo: ${e.message}")
            tvTimeZone.text = "⏰ Informações de Tempo: Erro\n${e.message}"
        }
    }

    /**
     * Verifica possíveis vazamentos WebRTC (simulado para Android)
     */
    private suspend fun checkWebRTCLeak() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando verificação WebRTC...")

        try {
            // Em Android, a verificação WebRTC completa é complexa e requer WebView
            // Esta é uma verificação simplificada
            val hasWebRTCSupport = try {
                Class.forName("org.webrtc.PeerConnectionFactory")
                true
            } catch (e: ClassNotFoundException) {
                false
            }

            val webRtcStatus = if (hasWebRTCSupport) {
                "⚠️ Disponível (pode vazar em browsers)"
            } else {
                "✅ Não detectado no app"
            }

            val explanation = "\nNota: Verificação limitada em Android.\n" +
                    "Vazamentos WebRTC são mais comuns em browsers."

            withContext(Dispatchers.Main) {
                tvWebRtc.text = "🌐 WebRTC: $webRtcStatus$explanation"
            }

            Log.d(TAG, "Verificação WebRTC concluída: $webRtcStatus")

        } catch (e: Exception) {
            Log.e(TAG, "Erro na verificação WebRTC: ${e.message}")
            withContext(Dispatchers.Main) {
                tvWebRtc.text = "🌐 WebRTC: Erro na verificação\n${e.message}"
            }
        }
    }

    /**
     * Verifica se o IP é detectado como pertencente a uma VPN
     */
    private suspend fun checkVPNDetection() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando verificação de detecção de VPN...")

        try {
            val ip = fetchPublicIP()
            if (!ip.startsWith("Erro") && ip.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))) {

                // Heurística simples baseada em faixas de IP comuns de VPN
                val isCommonVPNRange = ip.startsWith("10.") ||
                        ip.startsWith("172.16.") ||
                        ip.startsWith("172.17.") ||
                        ip.startsWith("172.18.") ||
                        ip.startsWith("172.19.") ||
                        ip.startsWith("172.20.") ||
                        ip.startsWith("172.21.") ||
                        ip.startsWith("172.22.") ||
                        ip.startsWith("172.23.") ||
                        ip.startsWith("172.24.") ||
                        ip.startsWith("172.25.") ||
                        ip.startsWith("172.26.") ||
                        ip.startsWith("172.27.") ||
                        ip.startsWith("172.28.") ||
                        ip.startsWith("172.29.") ||
                        ip.startsWith("172.30.") ||
                        ip.startsWith("172.31.") ||
                        ip.startsWith("192.168.") ||
                        ip.startsWith("100.") // CG-NAT comum em VPNs

                val detectionStatus = if (isCommonVPNRange) {
                    "⚠️ IP em faixa comum de VPNs"
                } else {
                    "✅ IP não identificado como VPN"
                }

                val detectionText = StringBuilder()
                detectionText.append("🛡️ Detecção VPN: $detectionStatus\n")
                detectionText.append("IP: $ip\n")
                detectionText.append("Faixa: ${if (isCommonVPNRange) "Privada/VPN" else "Pública"}")

                withContext(Dispatchers.Main) {
                    tvVpnDetection.text = detectionText.toString()
                }

                Log.d(TAG, "Verificação de detecção VPN concluída: $detectionStatus")

            } else {
                Log.w(TAG, "IP inválido para detecção de VPN: $ip")
                withContext(Dispatchers.Main) {
                    tvVpnDetection.text = "🛡️ Detecção VPN: IP inválido\n$ip"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na detecção de VPN: ${e.message}")
            withContext(Dispatchers.Main) {
                tvVpnDetection.text = "🛡️ Detecção VPN: Erro\n${e.message}"
            }
        }
    }
}