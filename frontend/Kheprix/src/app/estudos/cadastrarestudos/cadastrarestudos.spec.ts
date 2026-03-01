import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cadastrarestudos } from './cadastrarestudos';

describe('Cadastrarestudos', () => {
  let component: Cadastrarestudos;
  let fixture: ComponentFixture<Cadastrarestudos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Cadastrarestudos],
    }).compileComponents();

    fixture = TestBed.createComponent(Cadastrarestudos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
