import { Routes } from "@angular/router";
import { authGuard, guestGuard } from "./core/guards/auth.guard";

export const routes: Routes = [
  // Públicas
  {
    path: "",
    loadComponent: () =>
      import("./pages/home/home.component").then((m) => m.HomeComponent),
    canActivate: [guestGuard],
  },
  {
    path: "login",
    loadComponent: () =>
      import("./pages/login/login.component").then((m) => m.LoginComponent),
    canActivate: [guestGuard],
  },
  {
    path: "cadastro",
    loadComponent: () =>
      import("./pages/cadastro/cadastro.component").then(
        (m) => m.CadastroComponent,
      ),
    canActivate: [guestGuard],
  },
  {
    path: "recuperar-senha",
    loadComponent: () =>
      import("./pages/recuperar-senha/recuperar-senha.component").then(
        (m) => m.RecuperarSenhaComponent,
      ),
    canActivate: [guestGuard],
  },

  // Autenticadas
  {
    path: "inicio",
    loadComponent: () =>
      import("./pages/inicio/inicio.component").then((m) => m.InicioComponent),
    canActivate: [authGuard],
  },
  {
    path: "registro-rapido",
    loadComponent: () =>
      import("./pages/cadastro-rapido/cadastro-rapido.component").then(
        (m) => m.CadastroRapidoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "convites",
    loadComponent: () =>
      import("./pages/convites/convites.component").then(
        (m) => m.ConvitesComponent,
      ),
    //canActivate: [authGuard],
  },

  // Estudos
  {
    path: "estudos",
    loadComponent: () =>
      import("./pages/estudos/lista/estudos-lista.component").then(
        (m) => m.EstudosListaComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/analises",
    loadComponent: () =>
      import("./pages/estudos/analise/analise.component").then(
        (m) => m.AnalisesComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/novo",
    loadComponent: () =>
      import("./pages/estudos/novo/estudo-novo.component").then(
        (m) => m.EstudoNovoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id",
    loadComponent: () =>
      import("./pages/estudos/detalhe/estudo-detalhe.component").then(
        (m) => m.EstudoDetalheComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/editar",
    loadComponent: () =>
      import("./pages/estudos/editar/estudo-editar.component").then(
        (m) => m.EstudoEditarComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/colaboradores",
    loadComponent: () =>
      import("./pages/estudos/colaboradores/colaboradores.component").then(
        (m) => m.ColaboradoresComponent,
      ),
    canActivate: [authGuard],
  },

  // Espécies
  {
    path: "estudos/:estudo_id/especies",
    loadComponent: () =>
      import("./pages/especies/lista/especies-lista.component").then(
        (m) => m.EspeciesListaComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/especies/novo",
    loadComponent: () =>
      import("./pages/especies/novo/especie-novo.component").then(
        (m) => m.EspecieNovoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/especies/:especie_id",
    loadComponent: () =>
      import("./pages/especies/detalhe/especie-detalhe.component").then(
        (m) => m.EspecieDetalheComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/especies/:especie_id/editar",
    loadComponent: () =>
      import("./pages/especies/novo/especie-novo.component").then(
        (m) => m.EspecieNovoComponent,
      ),
    canActivate: [authGuard],
  },

  // Campanhas
  {
    path: "estudos/:estudo_id/campanhas",
    loadComponent: () =>
      import("./pages/campanhas/lista/campanhas-lista.component").then(
        (m) => m.CampanhasListaComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/novo",
    loadComponent: () =>
      import("./pages/campanhas/novo/campanha-novo.component").then(
        (m) => m.CampanhaNovoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/editar",
    loadComponent: () =>
      import("./pages/campanhas/novo/campanha-novo.component").then(
        (m) => m.CampanhaNovoComponent,
      ),
    canActivate: [authGuard],
  },

  // Unidades Amostrais
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades",
    loadComponent: () =>
      import("./pages/unidades/lista/unidades-lista.component").then(
        (m) => m.UnidadesListaComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/novo",
    loadComponent: () =>
      import("./pages/unidades/novo/unidade-novo.component").then(
        (m) => m.UnidadeNovoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/editar",
    loadComponent: () =>
      import("./pages/unidades/novo/unidade-novo.component").then(
        (m) => m.UnidadeNovoComponent,
      ),
    canActivate: [authGuard],
  },

  // Eventos de Amostragem
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/eventos",
    loadComponent: () =>
      import("./pages/eventos/lista/eventos-lista.component").then(
        (m) => m.EventosListaComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/eventos/novo",
    loadComponent: () =>
      import("./pages/eventos/novo/evento-novo.component").then(
        (m) => m.EventoNovoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/eventos/:evento_id/editar",
    loadComponent: () =>
      import("./pages/eventos/novo/evento-novo.component").then(
        (m) => m.EventoNovoComponent,
      ),
    canActivate: [authGuard],
  },

  // Registros de Ocorrência
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/eventos/:evento_id/registros",
    loadComponent: () =>
      import("./pages/registros/lista/registros-lista.component").then(
        (m) => m.RegistrosListaComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/eventos/:evento_id/registros/novo",
    loadComponent: () =>
      import("./pages/registros/novo/registro-novo.component").then(
        (m) => m.RegistroNovoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/eventos/:evento_id/registros/:registro_id/editar",
    loadComponent: () =>
      import("./pages/registros/novo/registro-novo.component").then(
        (m) => m.RegistroNovoComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: "estudos/:estudo_id/campanhas/:campanha_id/unidades/:unidade_id/eventos/:evento_id/registros/:registro_id",
    loadComponent: () =>
      import("./pages/registros/detalhe/registro-detalhe.component").then(
        (m) => m.RegistroDetalheComponent,
      ),
    canActivate: [authGuard],
  },

  {
    path: "chatbot",
    loadComponent: () => import("./pages/chatbot/chatbot.component").then((m) => m.ChatbotComponent),
    canActivate: [authGuard],
  },

  { path: "**", redirectTo: "" },
];
