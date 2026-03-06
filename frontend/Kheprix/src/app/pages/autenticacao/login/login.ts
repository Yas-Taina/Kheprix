import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { CommonModule } from "@angular/common";
import { NgxMaskDirective } from "ngx-mask";

@Component({
  selector: 'app-login',
  imports: [RouterModule, CommonModule, ReactiveFormsModule, NgxMaskDirective],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  loginForm: FormGroup;
  constructor(
    private fb: FormBuilder,
  ) {
    this.loginForm = this.fb.group({
      email: ["", [Validators.required, Validators.email, Validators.maxLength(100)]],
      senha: ["",[Validators.required, Validators.minLength(2), Validators.maxLength(20)]]
    });
  }
  onSubmit(): void{

  }
  ngOnInit(): void {
    if (this.loginForm.invalid) {
    return;
  }
  }
}
