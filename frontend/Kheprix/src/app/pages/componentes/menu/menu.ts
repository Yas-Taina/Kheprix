import { Component, DoCheck } from '@angular/core';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  standalone: true,
  selector: 'app-menu',
  imports: [CommonModule, RouterModule],
  templateUrl: './menu.html',
  styleUrls: ['./menu.css'],
})
export class Menu {
  constructor(private router: Router, private route: ActivatedRoute, private authService: AuthService) {}
  isOpen = false;
  toggleMenu(){
    this.isOpen = !this.isOpen
  }

  fazerLogout() {
    this.authService.logout();
  }

  abrirHome() { this.router.navigate(['/inicio']); }
  abrirRegistroRapido()   { this.router.navigate(['/registro-rapido']); }
  abrirVisualizarEstudos() { this.router.navigate(['/estudos']); }
  abrirCadastrarNovoEstudo()    { this.router.navigate(['/estudos/novo']); }
  abrirConvitesColaboracao()      { this.router.navigate(['/convites']); }
}
