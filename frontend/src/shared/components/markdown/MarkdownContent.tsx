import { Fragment, type ReactNode } from 'react';

/**
 * Hiển thị nội dung Markdown một cách AN TOÀN (không dùng dangerouslySetInnerHTML).
 *
 * Hỗ trợ:
 * - Khối code:  ```java ... ```
 * - Tiêu đề:    # / ## / ###
 * - Trích dẫn:  > ...
 * - Danh sách:  - mục  hoặc  1. mục
 * - Trong dòng: **đậm**, *nghiêng*, `code`, [link](https://...), ![ảnh](url), link trần https://...
 *
 * Ảnh chỉ được hiển thị nếu là https:// hoặc ảnh do chính hệ thống lưu (/api/post-images/...).
 */

// ===================== Kiểm tra URL an toàn =====================

const UPLOADED_IMAGE = /^\/api\/post-images\/[a-f0-9-]{36}\.(png|jpg)$/;

function safeLinkUrl(value: string): string | null {
  try {
    const url = new URL(value);
    return ['https:', 'http:', 'mailto:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

function safeImageUrl(value: string): string | null {
  if (UPLOADED_IMAGE.test(value)) return value;
  try {
    const url = new URL(value);
    return url.protocol === 'https:' ? url.href : null;
  } catch {
    return null;
  }
}

// ===================== Định dạng trong dòng =====================

// Thứ tự trong regex quan trọng: ảnh phải được bắt trước link (vì ảnh bắt đầu bằng "!")
const INLINE_PATTERN = /(!\[[^\]]*\]\([^\s)]+\)|\[[^\]]+\]\([^\s)]+\)|\*\*[^*\n]+\*\*|`[^`\n]+`|\*[^*\n]+\*|_[^_\n]+_|https?:\/\/[^\s<]+)/g;

function renderInline(text: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  INLINE_PATTERN.lastIndex = 0;

  while ((match = INLINE_PATTERN.exec(text))) {
    // Phần chữ thường nằm trước đoạn được định dạng
    if (match.index > lastIndex) nodes.push(text.slice(lastIndex, match.index));

    const token = match[0];
    const key = match.index;
    const image = token.match(/^!\[([^\]]*)\]\(([^\s)]+)\)$/);
    const link = token.match(/^\[([^\]]+)\]\(([^\s)]+)\)$/);

    if (image) {
      const src = safeImageUrl(image[2]);
      nodes.push(src
        ? <a key={key} className="md-image" href={src} target="_blank" rel="noopener noreferrer" title="Bấm để xem ảnh gốc">
            <img src={src} alt={image[1] || 'Ảnh minh hoạ'} loading="lazy" />
          </a>
        : token);
    } else if (link) {
      const href = safeLinkUrl(link[2]);
      nodes.push(href ? <a key={key} href={href} target="_blank" rel="noopener noreferrer">{link[1]}</a> : token);
    } else if (token.startsWith('**')) {
      nodes.push(<strong key={key}>{token.slice(2, -2)}</strong>);
    } else if (token.startsWith('`')) {
      nodes.push(<code key={key}>{token.slice(1, -1)}</code>);
    } else if (token.startsWith('*') || token.startsWith('_')) {
      nodes.push(<em key={key}>{token.slice(1, -1)}</em>);
    } else {
      const href = safeLinkUrl(token);
      nodes.push(href ? <a key={key} href={href} target="_blank" rel="noopener noreferrer">{token}</a> : token);
    }

    lastIndex = INLINE_PATTERN.lastIndex;
  }

  if (lastIndex < text.length) nodes.push(text.slice(lastIndex));
  return nodes;
}

// ===================== Các khối (đoạn văn, danh sách...) =====================

const isFence = (line: string) => /^\s*```/.test(line);
const isHeading = (line: string) => /^#{1,3}\s+/.test(line);
const isQuote = (line: string) => /^\s*>/.test(line);
const isBullet = (line: string) => /^\s*[-*+]\s+/.test(line);
const isNumbered = (line: string) => /^\s*\d+[.)]\s+/.test(line);

export default function MarkdownContent({ text, className = 'md-content' }: { text: string; className?: string }) {
  const lines = (text || '').replace(/\r/g, '').split('\n');
  const blocks: ReactNode[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Dòng trống: bỏ qua
    if (!line.trim()) { i++; continue; }

    // Khối code ``` ... ```
    if (isFence(line)) {
      const language = line.trim().slice(3).trim();
      const code: string[] = [];
      i++;
      while (i < lines.length && !isFence(lines[i])) code.push(lines[i++]);
      i++; // bỏ dòng ``` đóng
      blocks.push(<pre key={i} data-language={language || undefined}><code>{code.join('\n')}</code></pre>);
      continue;
    }

    // Tiêu đề: # -> h3, ## -> h4, ### -> h5 (không dùng h1/h2 vì trang đã có tiêu đề bài viết)
    if (isHeading(line)) {
      const level = line.match(/^#+/)![0].length;
      const Tag = (['h3', 'h4', 'h5'] as const)[level - 1];
      blocks.push(<Tag key={i}>{renderInline(line.replace(/^#+\s+/, ''))}</Tag>);
      i++;
      continue;
    }

    // Trích dẫn
    if (isQuote(line)) {
      const quote: string[] = [];
      while (i < lines.length && isQuote(lines[i])) quote.push(lines[i++].replace(/^\s*>\s?/, ''));
      blocks.push(<blockquote key={i}>{quote.map((q, n) => <p key={n}>{renderInline(q)}</p>)}</blockquote>);
      continue;
    }

    // Danh sách chấm đầu dòng
    if (isBullet(line)) {
      const items: string[] = [];
      while (i < lines.length && isBullet(lines[i])) items.push(lines[i++].replace(/^\s*[-*+]\s+/, ''));
      blocks.push(<ul key={i}>{items.map((item, n) => <li key={n}>{renderInline(item)}</li>)}</ul>);
      continue;
    }

    // Danh sách đánh số
    if (isNumbered(line)) {
      const items: string[] = [];
      while (i < lines.length && isNumbered(lines[i])) items.push(lines[i++].replace(/^\s*\d+[.)]\s+/, ''));
      blocks.push(<ol key={i}>{items.map((item, n) => <li key={n}>{renderInline(item)}</li>)}</ol>);
      continue;
    }

    // Đoạn văn: gom các dòng liền nhau, xuống dòng đơn giữ nguyên bằng <br>
    const paragraph: string[] = [];
    while (i < lines.length && lines[i].trim()
      && !isFence(lines[i]) && !isHeading(lines[i]) && !isQuote(lines[i]) && !isBullet(lines[i]) && !isNumbered(lines[i])) {
      paragraph.push(lines[i++]);
    }
    blocks.push(<p key={i}>{paragraph.map((p, n) => <Fragment key={n}>{n > 0 && <br />}{renderInline(p)}</Fragment>)}</p>);
  }

  return <div className={className}>{blocks}</div>;
}
