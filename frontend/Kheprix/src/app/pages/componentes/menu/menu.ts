import { Component, DoCheck } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUser } from '@fortawesome/free-regular-svg-icons';

@Component({
  selector: 'app-menu',
  imports: [CommonModule, RouterModule, FontAwesomeModule],
  templateUrl: './menu.html',
  styleUrl: './menu.css',
})
export class Menu {
  faUser = faUser;
  isOpen = false;
  toggleMenu(){
    this.isOpen = !this.isOpen
  }
}
