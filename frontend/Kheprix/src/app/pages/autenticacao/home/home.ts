import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
})
export class Home {
  constructor(private router: Router) {}
  goLogin()    { this.router.navigate(['/login']); }
  goCadastro() { this.router.navigate(['/cadastro']); }
}
