import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class UtilService {

  /**
   * Converte decimal para DMS (graus, minutos, segundos)
   * Formato: -25°42'48"
   */
  decimalToDMS(decimal: number): string {
    const sign = decimal < 0 ? '-' : '';
    const abs = Math.abs(decimal);
    const degrees = Math.floor(abs);
    const minutesFloat = (abs - degrees) * 60;
    const minutes = Math.floor(minutesFloat);
    const seconds = Math.round((minutesFloat - minutes) * 60);
    return `${sign}${degrees}°${minutes}'${seconds}"`;
  }

  /**
   * Converte DMS para decimal
   * Aceita formatos: -25°42'48" ou 25°42'48"
   */
  dmsTodecimal(dms: string): number {
    const negative = dms.trim().startsWith('-');
    const clean = dms.replace(/[°'"−-]/g, ' ').trim();
    const parts = clean.split(/\s+/).filter(Boolean);
    const degrees = parseFloat(parts[0]) || 0;
    const minutes = parseFloat(parts[1]) || 0;
    const seconds = parseFloat(parts[2]) || 0;
    const decimal = degrees + minutes / 60 + seconds / 3600;
    return negative ? -decimal : decimal;
  }

  /**
   * Obtém a localização atual do dispositivo como DMS
   */
  getCurrentLocationDMS(): Promise<{ latDMS: string; lngDMS: string; latDecimal: number; lngDecimal: number }> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocalização não suportada'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude;
          const lng = pos.coords.longitude;
          resolve({
            latDMS: this.decimalToDMS(lat),
            lngDMS: this.decimalToDMS(lng),
            latDecimal: lat,
            lngDecimal: lng,
          });
        },
        (err) => reject(err),
        { enableHighAccuracy: true, timeout: 10000 }
      );
    });
  }

  /**
   * Converte File para base64 com header data:image/...;base64,...
   */
  fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  /**
   * Captura foto pela câmera
   */
  openCamera(callback: (base64: string) => void): void {
  const isMobile = /Android|iPhone|iPad/i.test(navigator.userAgent);

  if (isMobile) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.capture = 'environment';

    input.onchange = async () => {
      if (input.files && input.files[0]) {
        const b64 = await this.fileToBase64(input.files[0]);
        callback(b64);
      }
    };

    input.click();
    return;
  }

  navigator.mediaDevices.getUserMedia({ video: true })
    .then(stream => {

      const overlay = document.createElement('div');
      overlay.style.position = 'fixed';
      overlay.style.top = '0';
      overlay.style.left = '0';
      overlay.style.width = '100%';
      overlay.style.height = '100%';
      overlay.style.background = 'rgba(0,0,0,0.7)';
      overlay.style.display = 'flex';
      overlay.style.alignItems = 'center';
      overlay.style.justifyContent = 'center';
      overlay.style.zIndex = '9999';

      const modal = document.createElement('div');
      modal.style.background = '#fff';
      modal.style.borderRadius = '12px';
      modal.style.padding = '16px';
      modal.style.display = 'flex';
      modal.style.flexDirection = 'column';
      modal.style.alignItems = 'center';
      modal.style.gap = '12px';
      modal.style.maxWidth = '90%';
      modal.style.boxShadow = '0 10px 30px rgba(0,0,0,0.3)';

      const video = document.createElement('video');
      video.autoplay = true;
      video.style.width = '100%';
      video.style.borderRadius = '8px';
      video.srcObject = stream;

      const actions = document.createElement('div');
      actions.style.display = 'flex';
      actions.style.gap = '10px';

      const captureBtn = document.createElement('button');
      captureBtn.innerText = 'Capturar';
      captureBtn.style.padding = '8px 16px';
      captureBtn.style.border = 'none';
      captureBtn.style.borderRadius = '6px';
      captureBtn.style.background = '#4CAF50';
      captureBtn.style.color = '#fff';
      captureBtn.style.cursor = 'pointer';

      const cancelBtn = document.createElement('button');
      cancelBtn.innerText = 'Cancelar';
      cancelBtn.style.padding = '8px 16px';
      cancelBtn.style.border = 'none';
      cancelBtn.style.borderRadius = '6px';
      cancelBtn.style.background = '#f44336';
      cancelBtn.style.color = '#fff';
      cancelBtn.style.cursor = 'pointer';

      actions.appendChild(captureBtn);
      actions.appendChild(cancelBtn);

      modal.appendChild(video);
      modal.appendChild(actions);
      overlay.appendChild(modal);
      document.body.appendChild(overlay);

      const close = () => {
        stream.getTracks().forEach(track => track.stop());
        overlay.remove();
      };

      cancelBtn.onclick = () => {
        close();
      };

      captureBtn.onclick = () => {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        const ctx = canvas.getContext('2d');
        ctx?.drawImage(video, 0, 0);

        const base64 = canvas.toDataURL('image/png');

        close();
        callback(base64);
      };
    })
    .catch(err => {
      console.error('Erro ao acessar webcam:', err);

      this.openFilePicker((b64) => callback(b64));
    });
}


  /**
   * Abre o seletor de arquivos do dispositivo
   */
  openFilePicker(callback: (base64: string, filename: string) => void): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/jpeg,image/png,image/webp';
    input.onchange = async () => {
      if (input.files && input.files[0]) {
        const b64 = await this.fileToBase64(input.files[0]);
        callback(b64, input.files[0].name);
      }
    };
    input.click();
  }

  /**
   * Constrói URL completa de foto a partir do path retornado pela API
   */
  buildFotoUrl(apiUrl: string, fotoPath: string): string {
    if (!fotoPath) return '';
    if (fotoPath.startsWith('http')) return fotoPath;
    return `${apiUrl}${fotoPath}`;
  }

  /**
   * Formata data ISO para dd/mm/yyyy
   */
  formatDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
  }

  /**
   * Formata datetime ISO para dd/mm/yyyy HH:mm
   */
  formatDateTime(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return `${this.formatDate(iso)} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  }

  /**
   * Retorna a cor CSS correspondente ao status de conservação
   */
  conservacaoColor(status: string): string {
    const map: Record<string, string> = {
      LC: 'var(--color-conserv-lc)',
      NT: 'var(--color-conserv-nt)',
      VU: 'var(--color-conserv-vu)',
      EN: 'var(--color-conserv-en)',
      CR: 'var(--color-conserv-cr)',
      EW: 'var(--color-conserv-ew)',
      EX: 'var(--color-conserv-ex)',
      DD: 'var(--color-conserv-dd)',
    };
    return map[status] || 'var(--color-text-muted)';
  }
}
