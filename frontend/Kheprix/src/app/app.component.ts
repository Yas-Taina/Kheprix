import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Menu } from './pages/componentes/menu/menu';
import { Footer } from './pages/componentes/footer/footer';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';

@Component({
  standalone: true,
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule, Menu, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})

export class App implements OnInit {
  constructor(private authService: AuthService) {}
  isLogged = false;
  protected readonly title = signal('Kheprix');

  ngOnInit(){
    this.isLogged = this.authService.isLoggedIn();
  }

}