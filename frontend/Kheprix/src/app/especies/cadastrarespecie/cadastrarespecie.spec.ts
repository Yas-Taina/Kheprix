import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cadastrarespecie } from './cadastrarespecie';

describe('Cadastrarespecie', () => {
  let component: Cadastrarespecie;
  let fixture: ComponentFixture<Cadastrarespecie>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Cadastrarespecie],
    }).compileComponents();

    fixture = TestBed.createComponent(Cadastrarespecie);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
