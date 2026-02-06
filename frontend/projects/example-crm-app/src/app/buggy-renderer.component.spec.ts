import { TestBed } from '@angular/core/testing';
import { BuggyRendererComponent } from './buggy-renderer.component';

describe('BuggyRendererComponent', () => {
  it('should throw during construction', () => {
    expect(() => {
      TestBed.configureTestingModule({
        imports: [BuggyRendererComponent],
      });
      TestBed.createComponent(BuggyRendererComponent);
    }).toThrow('BuggyRendererComponent intentionally fails!');
  });
});
