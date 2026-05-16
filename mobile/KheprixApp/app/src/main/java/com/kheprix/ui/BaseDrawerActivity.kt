package com.kheprix.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.models.TokenRequest
import kotlinx.coroutines.launch


abstract class BaseDrawerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    protected var drawerLayout: DrawerLayout? = null
    protected var navigationView: NavigationView? = null

    private var offlineBanner: View? = null
    private var dialogTokenInvalidoVisivel = false

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { atualizarBannerOffline(false) }
        }
        override fun onLost(network: Network) {
            runOnUiThread { atualizarBannerOffline(true) }
        }
    }

    override fun onStart() {
        super.onStart()
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try { connectivityManager.registerNetworkCallback(request, networkCallback) } catch (_: Exception) {}
        atualizarBannerOffline(!estaOnline())
    }

    override fun onStop() {
        super.onStop()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        verificarTokenOnline()
        atualizarEmailDrawer()
    }

    private fun atualizarEmailDrawer() {
        val header = navigationView?.getHeaderView(0) ?: return
        header.findViewById<TextView>(R.id.tvDrawerEmail)?.text =
            SessionManager.getUserEmail() ?: ""
    }

    private fun estaOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun atualizarBannerOffline(offline: Boolean) {
        offlineBanner?.visibility = if (offline) View.VISIBLE else View.GONE
        onEstadoOffline(offline)
    }

    protected open fun onEstadoOffline(offline: Boolean) {}

    private fun verificarTokenOnline() {
        val token = SessionManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val resposta = RetrofitClient.apiService.validarToken(TokenRequest(token))
                if (resposta.isSuccessful && resposta.body()?.valido == false) {
                    mostrarDialogTokenInvalido()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun mostrarDialogTokenInvalido() {
        if (dialogTokenInvalidoVisivel || isFinishing) return
        dialogTokenInvalidoVisivel = true

        AlertDialog.Builder(this)
            .setTitle("Você está online")
            .setMessage("Sua sessão expirou. Faça login novamente para continuar.")
            .setPositiveButton("OK") { _, _ ->
                dialogTokenInvalidoVisivel = false
                SessionManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    override fun setContentView(view: View) {
        val instalado = installDrawer(view)
        super.setContentView(instalado)
    }

    override fun setContentView(layoutResID: Int) {
        setContentView(layoutInflater.inflate(layoutResID, null))
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        view.layoutParams = params
        setContentView(view)
    }

    private fun installDrawer(content: View): View {
        val banner = criarBannerOffline()
        offlineBanner = banner

        if (content is DrawerLayout) {
            // DrawerLayout já é a raiz (ex: HomeActivity) — injeta o banner dentro do
            // filho de conteúdo para preservar o fitsSystemWindows do DrawerLayout.
            content.findViewById<NavigationView>(R.id.navigationView)?.let {
                navigationView = it
                it.setNavigationItemSelectedListener(this)
            }
            for (i in 0 until content.childCount) {
                val filho = content.getChildAt(i)
                if (filho is NavigationView) continue
                content.removeViewAt(i)
                val wrapper = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    addView(banner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    addView(filho, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
                }
                content.addView(wrapper, i, DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                break
            }
            drawerLayout = content
            return content
        }

        val drawer = DrawerLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            fitsSystemWindows = true
        }
        drawer.addView(content, DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val navCtx = android.view.ContextThemeWrapper(this, R.style.ThemeOverlay_Kheprix_NavigationView)
        val nav = NavigationView(navCtx).apply {
            id = View.generateViewId()
            background = ColorDrawable(Color.parseColor("#EDE9DE"))
            fitsSystemWindows = true
            inflateMenu(R.menu.drawer_menu)
            inflateHeaderView(R.layout.nav_drawer_header)
            itemTextColor = ColorStateListSimples(Color.parseColor("#4A5240"))
            itemIconTintList = ColorStateListSimples(Color.parseColor("#6B7A5E"))
            setNavigationItemSelectedListener(this@BaseDrawerActivity)
        }
        val widthPx = (280 * resources.displayMetrics.density).toInt()
        drawer.addView(
            nav,
            DrawerLayout.LayoutParams(widthPx, MATCH_PARENT).apply { gravity = Gravity.START }
        )
        navigationView = nav
        drawerLayout = drawer

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            addView(banner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(drawer, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }
    }

    private fun criarBannerOffline(): LinearLayout {
        val dp = resources.displayMetrics.density
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#C62828"))
            visibility = View.GONE
            val pV = (6 * dp).toInt()
            val pH = (16 * dp).toInt()
            setPadding(pH, pV, pH, pV)

            val icon = ImageView(this@BaseDrawerActivity).apply {
                setImageResource(R.drawable.ic_wifi_off)
                imageTintList = ColorStateListSimples(Color.WHITE)
                val size = (18 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size)
            }
            addView(icon)

            val texto = TextView(this@BaseDrawerActivity).apply {
                text = "Você está offline"
                setTextColor(Color.WHITE)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    marginStart = (8 * dp).toInt()
                }
            }
            addView(texto)
        }
    }

    protected fun setupDrawer(drawer: DrawerLayout, navView: NavigationView) {
        drawerLayout = drawer
        navigationView = navView
        navView.setNavigationItemSelectedListener(this)
    }

    fun openDrawer() {
        drawerLayout?.openDrawer(GravityCompat.START)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout?.closeDrawer(GravityCompat.START)
        return when (item.itemId) {
            R.id.nav_perfil -> {
                if (this !is PerfilActivity)
                    startActivity(Intent(this, PerfilActivity::class.java))
                true
            }

            R.id.nav_home -> {
                if (this !is HomeActivity)
                    startActivity(Intent(this, HomeActivity::class.java))
                true
            }

            R.id.nav_registro_rapido -> {
                if (this !is RegistroRapidoActivity)
                    startActivity(Intent(this, RegistroRapidoActivity::class.java))
                true
            }

            R.id.nav_estudos -> {
                if (this !is EstudosActivity)
                    startActivity(Intent(this, EstudosActivity::class.java))
                true
            }

            R.id.nav_novo_estudo -> {
                if (this !is NovoEstudoActivity)
                    startActivity(Intent(this, NovoEstudoActivity::class.java))
                true
            }

            R.id.nav_convites -> {
                if (this !is ConvitesActivity)
                    startActivity(Intent(this, ConvitesActivity::class.java))
                true
            }

            R.id.nav_logout -> {
                SessionManager.logout()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                true
            }

            else -> false
        }
    }

    override fun onBackPressed() {
        if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

@Suppress("FunctionName")
private fun ColorStateListSimples(color: Int): android.content.res.ColorStateList =
    android.content.res.ColorStateList.valueOf(color)
