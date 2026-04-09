import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EstudosService } from '../../../services/estudos.service';
import { AutenticacaoService } from '../../../services/autenticacao.service';
import { EstudoListItem } from '../../../services/modelos/estudo.model';


@Component({
  selector: 'app-estudos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './estudos.component.html',
  styleUrls: ['./estudos.component.css'],
})
export class EstudosComponent implements OnInit {
  estudos: EstudoListItem[] = [];
  filtrados: EstudoListItem[] = [];
  busca = '';
  campoBusca = 'nome';
  carregando = true;
  erro = '';
  toast = '';

  constructor(
    private estudosService: EstudosService,
    private auth: AutenticacaoService,
    private router: Router,
  ) {}

  ngOnInit() { this.carregar(); }

  carregar() {
    this.carregando = true;
    this.estudosService.listar().subscribe({
      next: (data) => { this.estudos = data; this.filtrados = data; this.carregando = false; },
      error: () => { this.erro = 'Erro ao carregar estudos.'; this.carregando = false; },
    });
  }

  pesquisar() {
    const q = this.busca.toLowerCase();
    this.filtrados = this.estudos.filter(e => e.nome.toLowerCase().includes(q));
  }

  novoEstudo() { this.router.navigate(['/estudos/novo']); }

  abrirEstudo(e: EstudoListItem) { this.router.navigate(['/estudos', e.id, 'campanhas']); }

  deletar(e: EstudoListItem, event: Event) {
    event.stopPropagation();
    if (!confirm(`Deletar estudo "${e.nome}"?`)) return;
    this.estudosService.deletar(e.id).subscribe({
      next: () => { this.showToast('Estudo removido.'); this.carregar(); },
      error: () => this.showToast('Erro ao deletar.'),
    });
  }

  isProprietario(e: EstudoListItem) { return e.perfil === 'proprietario'; }

  irColaboradores(e: EstudoListItem, event: Event) {
    event.stopPropagation();
    // Página ainda não implementada — placeholder
    this.showToast('Página de colaboradores em breve.');
  }

  formatData(d: string) {
    return new Date(d).toLocaleDateString('pt-BR');
  }

  showToast(msg: string) {
    this.toast = msg;
    setTimeout(() => this.toast = '', 3000);
  }

  voltar() { this.router.navigate(['/']); }
}
