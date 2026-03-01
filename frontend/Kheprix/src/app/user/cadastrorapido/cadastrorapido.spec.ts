import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cadastrorapido } from './cadastrorapido';

describe('Cadastrorapido', () => {
  let component: Cadastrorapido;
  let fixture: ComponentFixture<Cadastrorapido>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Cadastrorapido],
    }).compileComponents();

    fixture = TestBed.createComponent(Cadastrorapido);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
