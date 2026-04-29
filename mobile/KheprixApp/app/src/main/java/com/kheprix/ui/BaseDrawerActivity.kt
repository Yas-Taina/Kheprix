package com.kheprix.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.kheprix.R
import com.kheprix.api.SessionManager

/**
 * Activity base que injeta o menu lateral (Navigation Drawer) em qualquer
 * layout, sem exigir que o XML tenha um DrawerLayout.
 *
 * Uso: a activity herda de BaseDrawerActivity e chama setContentView normalmente
 * (com binding.root, layoutResId ou View). O drawer é montado em volta do
 * conteúdo, com o menu padrão (drawer_menu.xml) e o cabeçalho (nav_drawer_header).
 *
 * Para abrir o drawer ao clicar no botão hamburguer da activity:
 *     binding.ivMenuLateral.setOnClickListener { openDrawer() }
 *
 * Se o layout original já é um DrawerLayout (como activity_home.xml), o wrap
 * é dispensado e o NavigationView interno é reutilizado.
 */
abstract class BaseDrawerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    protected var drawerLayout: DrawerLayout? = null
    protected var navigationView: NavigationView? = null

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
        if (content is DrawerLayout) {
            drawerLayout = content
            content.findViewById<NavigationView>(R.id.navigationView)?.let {
                navigationView = it
                it.setNavigationItemSelectedListener(this)
            }
            return content
        }

        val drawer = DrawerLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            fitsSystemWindows = true
        }
        drawer.addView(content, DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val nav = NavigationView(this).apply {
            id = View.generateViewId()
            background = ColorDrawable(Color.parseColor("#EDE9DE"))
            fitsSystemWindows = true
            inflateMenu(R.menu.drawer_menu)
            inflateHeaderView(R.layout.nav_drawer_header)
            itemTextColor = ColorStateListSimples(Color.parseColor("#4A5240"))
            itemIconTintList = ColorStateListSimples(Color.parseColor("#6B7A5E"))
            // setSubheaderColor exige Material Components 1.12.0+. Reflection
            // mantem compativel: em versoes antigas, simplesmente nao aplica.
            try {
                NavigationView::class.java
                    .getMethod("setSubheaderColor", android.content.res.ColorStateList::class.java)
                    .invoke(this, ColorStateListSimples(Color.parseColor("#4A5240")))
            } catch (_: Throwable) { }
            setNavigationItemSelectedListener(this@BaseDrawerActivity)
        }
        val widthPx = (280 * resources.displayMetrics.density).toInt()
        drawer.addView(
            nav,
            DrawerLayout.LayoutParams(widthPx, MATCH_PARENT).apply { gravity = Gravity.START }
        )
        drawerLayout = drawer
        navigationView = nav
        return drawer
    }

    /** Helper de compatibilidade: HomeActivity ainda chama isto. No-op. */
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

/** ColorStateList que retorna a mesma cor pra todos os estados. */
@Suppress("FunctionName")
private fun ColorStateListSimples(color: Int): android.content.res.ColorStateList =
    android.content.res.ColorStateList.valueOf(color)
