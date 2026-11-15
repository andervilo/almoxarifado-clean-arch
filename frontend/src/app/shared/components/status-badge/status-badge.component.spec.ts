import { ComponentRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<StatusBadgeComponent>;
  let componentRef: ComponentRef<StatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(StatusBadgeComponent);
    componentRef = fixture.componentRef;
  });

  it('formata status e aplica tom semantico', () => {
    componentRef.setInput('status', 'PARCIALMENTE_ENTREGUE');
    fixture.detectChanges();

    const span = fixture.nativeElement.querySelector('.badge') as HTMLElement;
    expect(span.textContent?.trim()).toBe('PARCIALMENTE ENTREGUE');
    expect(span.getAttribute('data-tone')).toBe('warning');
  });
});
