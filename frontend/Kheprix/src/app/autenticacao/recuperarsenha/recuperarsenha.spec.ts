import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Recuperarsenha } from './recuperarsenha';

describe('Recuperarsenha', () => {
  let component: Recuperarsenha;
  let fixture: ComponentFixture<Recuperarsenha>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Recuperarsenha],
    }).compileComponents();

    fixture = TestBed.createComponent(Recuperarsenha);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
