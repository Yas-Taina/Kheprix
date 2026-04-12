import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EspeciesService } from '../../../services/especies.service';


@Component({
  selector: 'app-cadastro-especie',
  standalone: true,
  imports: [CommonModule, FormsModule ],
  templateUrl: './cadastrarespecie.html',
  styleUrls: ['./cadastrarespecie.css'],
})
export class CadastrarEspecie implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('videoEl')  videoEl!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvasEl') canvasEl!: ElementRef<HTMLCanvasElement>;

  estudoId!: number;

  foto = '';
  nomeArquivoFoto = '';
  classe = '';
  ordem = '';
  familia = '';
  genero = '';
  especie = '';
  nomePopular = '';
  statusConservacao = 'Ameaçada';
  endemismo = false;

  mostrarCamera = false;
  streamCamera: MediaStream | null = null;

  statusOptions = ['Ameaçada', 'Vulnerável', 'Quase Ameaçada', 'Pouco Preocupante', 'Dados Insuficientes', 'Extinta'];

  erro = '';
  carregando = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private especiesService: EspeciesService,
  ) {}

  ngOnInit() {
    this.estudoId = Number(this.route.snapshot.paramMap.get('estudoId'));
  }

  /* ── Arquivo ── */
  abrirArquivo() { this.fileInput.nativeElement.click(); }

  onArquivoSelecionado(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.nomeArquivoFoto = input.files[0].name;
      // Mock: envia string fixa ao backend
      this.foto = 'foto_mockada.jpeg';
    }
  }

  /* ── Câmera ── */
  async abrirCamera() {
    this.mostrarCamera = true;
    try {
      this.streamCamera = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      setTimeout(() => {
        if (this.videoEl) this.videoEl.nativeElement.srcObject = this.streamCamera;
      }, 100);
    } catch {
      this.erro = 'Não foi possível acessar a câmera.';
      this.mostrarCamera = false;
    }
  }

  tirarFoto() {
    const video  = this.videoEl.nativeElement;
    const canvas = this.canvasEl.nativeElement;
    canvas.width  = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d')!.drawImage(video, 0, 0);
    this.nomeArquivoFoto = 'foto_camera.jpeg';
    // Mock: envia string fixa ao backend
    this.foto = 'foto_camera_mockada.jpeg';
    this.fecharCamera();
  }

  fecharCamera() {
    this.streamCamera?.getTracks().forEach(t => t.stop());
    this.streamCamera = null;
    this.mostrarCamera = false;
  }

  /* ── Submit ── */
  confirmar() {
    if (!this.classe || !this.ordem || !this.familia || !this.genero || !this.especie) {
      this.erro = 'Preencha os campos obrigatórios (Classe, Ordem, Família, Gênero, Espécie).';
      return;
    }
    this.carregando = true;
    this.erro = '';
    this.especiesService.criar(this.estudoId, {
      classe: this.classe,
      ordem: this.ordem,
      familia: this.familia,
      genero: this.genero,
      especie: this.especie,
      endemismo: this.endemismo,
      foto: this.foto || undefined,
      nome_popular: this.nomePopular || undefined,
      status_conservacao: this.statusConservacao || undefined,
    }).subscribe({
      next: () => this.router.navigate(['/estudos', this.estudoId, 'especies']),
      error: () => { this.erro = 'Erro ao salvar espécie.'; this.carregando = false; },
    });
  }

  voltar() { this.router.navigate(['/estudos', this.estudoId, 'especies']); }
}
