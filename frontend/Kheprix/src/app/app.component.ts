import { Component, signal } from "@angular/core";
import { Router, RouterOutlet } from "@angular/router";
import { Menu } from "./pages/componentes/menu/menu";
import { Footer } from "./pages/componentes/footer/footer";
import { CommonModule } from "@angular/common";
import { AuthService } from "./core/services/auth.service";

@Component({
  standalone: true,
  selector: "app-root",
  imports: [RouterOutlet, CommonModule, Menu, Footer],
  templateUrl: "./app.html",
  styleUrl: "./app.css",
})
export class App {
  protected readonly title = signal("Kheprix");

  private readonly rotasPublicas = ["", "login", "cadastro", "recuperar-senha"];

  constructor(
    public router: Router,
    private authService: AuthService,
  ) {}

  get isLogged(): boolean {
    return this.authService.isLoggedIn();
  }

  mostrarFab(): boolean {
    const segmento = this.router.url.split("/")[1]?.split("?")[0] ?? "";
    return !this.rotasPublicas.includes(segmento) && segmento !== "chatbot";
  }
}
