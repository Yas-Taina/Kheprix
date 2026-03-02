import { Routes } from '@angular/router';
import { Home } from './autenticacao/home/home';
import { Login } from './autenticacao/login/login';
import { Cadastro } from './autenticacao/cadastro/cadastro';
import { Recuperarsenha } from './autenticacao/recuperarsenha/recuperarsenha';

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
