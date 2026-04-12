package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.kheprix.R
import com.kheprix.api.SessionManager

/**
 * BaseDrawerActivity — Activity base com menu lateral (Navigation Drawer).
 *
 * Todas as activities principais devem herdar desta classe e usar
 * o layout activity_base_drawer.xml como raiz (com DrawerLayout).
 *
 * O menu hamburguer abre o drawer com os 4 links principais + Logout.
 *
 * Para usar: sua activity herda BaseDrawerActivity e chama
 *   setupDrawer(drawerLayout, navigationView)
 * após o setContentView.
 */
abstract class BaseDrawerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    protected var drawerLayout: DrawerLayout? = null

    protected fun setupDrawer(drawer: DrawerLayout, navView: NavigationView) {
        this.drawerLayout = drawer
        navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout?.closeDrawer(GravityCompat.START)
        return when (item.itemId) {
            R.id.nav_perfil -> {
                startActivity(Intent(this, PerfilActivity::class.java)); true
            }
            R.id.nav_home -> {
                if (this !is HomeActivity)
                    startActivity(Intent(this, HomeActivity::class.java))
                true
            }
            R.id.nav_registro_rapido -> {
                startActivity(Intent(this, RegistroRapidoActivity::class.java)); true
            }
            R.id.nav_estudos -> {
                startActivity(Intent(this, EstudosActivity::class.java)); true
            }
            R.id.nav_novo_estudo -> {
                startActivity(Intent(this, NovoEstudoActivity::class.java)); true
            }
            R.id.nav_convites -> {
                startActivity(Intent(this, ConvitesActivity::class.java)); true
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
