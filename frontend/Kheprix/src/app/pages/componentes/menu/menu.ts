import { Component, DoCheck, OnInit } from "@angular/core";
import { Router, RouterModule, ActivatedRoute } from "@angular/router";
import { CommonModule } from "@angular/common";
import { AuthService } from "../../../core/services/auth.service";

@Component({
  standalone: true,
  selector: "app-menu",
  imports: [CommonModule, RouterModule],
  templateUrl: "./menu.html",
  styleUrls: ["./menu.css"],
})
export class Menu implements OnInit {
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
  ) {}
  isLogged = false;
  isOpen = false;
  isInicio = false;

  ngOnInit(): void {
    this.checkLogged();
    this.router.events.subscribe(() => {
      this.isInicio = this.router.url.includes('/inicio');
    });
  }

  checkLogged() {
    this.isLogged = this.authService.isLoggedIn();
  }

  toggleMenu() {
    this.isOpen = !this.isOpen;
  }

  fazerLogout() {
    this.authService.logout();
  }

  abrirHome() {
    this.router.navigate(["/inicio"]);
    this.toggleMenu();
  }
  abrirRegistroRapido() {
    this.router.navigate(["/registro-rapido"]);
    this.toggleMenu();
  }
  abrirVisualizarEstudos() {
    this.router.navigate(["/estudos"]);
    this.toggleMenu();
  }
  abrirCadastrarNovoEstudo() {
    this.router.navigate(["/estudos/novo"]);
    this.toggleMenu();
  }
  abrirConvitesColaboracao() {
    this.router.navigate(["/convites"]);
    this.toggleMenu();
  }
}
