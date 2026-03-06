import { Routes } from '@angular/router';
import { Home } from './pages/autenticacao/home/home';
import { Login } from './pages/autenticacao/login/login';
import { Cadastro } from './pages/autenticacao/cadastro/cadastro';
import { Recuperarsenha } from './pages/autenticacao/recuperarsenha/recuperarsenha';

export const routes: Routes = [
    //Rotas publicas
    {
        path: "home",
        component: Home
    },

    {
        path: "login",
        component: Login
    },
    {
        path: "cadastro",
        component: Cadastro
    },
    {
        path: "recuperarsenha",
        component: Recuperarsenha
    },

    //Redirecionamento
    { path: "", redirectTo: "home", pathMatch: "full" }, 
    { path: "**", redirectTo: "home", pathMatch: "full" },
];
