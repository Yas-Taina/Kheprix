import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { CommonModule } from "@angular/common";
import { NgxMaskDirective } from "ngx-mask";


@Component({
  selector: 'app-cadastro',
  imports: [RouterModule, CommonModule, ReactiveFormsModule, NgxMaskDirective],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css',
})
export class Cadastro implements OnInit {
  cadastroForm: FormGroup;
  constructor(
    private fb: FormBuilder,
  ) {
    this.cadastroForm = this.fb.group({
      nome: ["", [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ["", [Validators.required, Validators.email, Validators.maxLength(100)]],
      senha: ["",[Validators.required, Validators.minLength(2), Validators.maxLength(20)]],
      confirmarsenha: ["", [Validators.required, Validators.minLength(2), Validators.maxLength(20)]]
    });
  }
  onSubmit(): void{

  }
  ngOnInit(): void {
    if (this.cadastroForm.invalid) {
    return;
  }
  }
}
