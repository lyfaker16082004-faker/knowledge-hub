import { Fragment, type ReactNode } from 'react';

/*
 * Hiển thị phần "Mô tả chi tiết" của tài liệu.
 * Mô tả có thể ở 2 dạng:
 *  1. HTML (soạn bằng trình soạn thảo Summernote) -> lọc bỏ thẻ nguy hiểm rồi hiển thị.
 *  2. Văn bản kiểu Markdown đơn giản -> tự chuyển thành tiêu đề, danh sách, đoạn văn.
 */

// =========================================================
// PHẦN 1: KIỂM TRA ĐƯỜNG DẪN AN TOÀN
// =========================================================

/**
 * Chỉ cho phép đường dẫn an toàn:
 * - Ảnh: chỉ https
 * - Liên kết: https, http hoặc mailto
 * Trả về null nếu không hợp lệ (ví dụ "javascript:..." bị chặn).
 */
function safeUrl(value: string, isImage = false) {
  try {
    const url = new URL(value);
    if (isImage) {
      return url.protocol === 'https:' ? url.href : null;
    }
    if (url.protocol === 'https:' || url.protocol === 'http:' || url.protocol === 'mailto:') {
      return url.href;
    }
    return null;
  } catch {
    return null;
  }
}

// =========================================================
// PHẦN 2: ĐỊNH DẠNG TRONG DÒNG (in đậm, nghiêng, code, link, ảnh)
// =========================================================

type Token = {
  kind: 'text' | 'bold' | 'italic' | 'code' | 'link' | 'image';
  text?: string;
  url?: string;
};

// Tìm: ![ảnh](url) | [link](url) | **đậm** | *nghiêng* | `code` | http://...
const INLINE_PATTERN = /(!?\[[^\]]*\]\([^\s)]+\)|\*\*.+?\*\*|\*[^*\n]+\*|`[^`\n]+`|https?:\/\/[^\s]+)/g;
const MARKDOWN_LINK = /^(!?)\[([^\]]*)\]\(([^\s)]+)\)$/;

/** Tách một dòng chữ thành các mảnh (token) theo định dạng. */
function splitInline(source: string): Token[] {
  const result: Token[] = [];
  const pattern = new RegExp(INLINE_PATTERN);
  let last = 0;
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(source)) !== null) {
    // Phần chữ thường nằm trước đoạn có định dạng
    if (match.index > last) {
      result.push({ kind: 'text', text: source.slice(last, match.index) });
    }

    const token = match[0];
    const link = token.match(MARKDOWN_LINK);

    if (link) {
      // [chữ](url) hoặc ![chữ](url)
      const isImage = link[1] === '!';
      const url = safeUrl(link[3], isImage);
      if (url) {
        result.push({ kind: isImage ? 'image' : 'link', text: link[2], url });
      } else {
        result.push({ kind: 'text', text: token });
      }
    } else if (token.startsWith('**')) {
      result.push({ kind: 'bold', text: token.slice(2, -2) });
    } else if (token.startsWith('*')) {
      result.push({ kind: 'italic', text: token.slice(1, -1) });
    } else if (token.startsWith('`')) {
      result.push({ kind: 'code', text: token.slice(1, -1) });
    } else {
      // Đường dẫn viết thẳng: http://...
      const url = safeUrl(token);
      if (url) {
        result.push({ kind: 'link', text: token, url });
      } else {
        result.push({ kind: 'text', text: token });
      }
    }

    last = pattern.lastIndex;
  }

  // Phần chữ còn lại ở cuối
  if (last < source.length) {
    result.push({ kind: 'text', text: source.slice(last) });
  }
  return result;
}

/** Đổi các token thành thẻ React. */
function inlineNodes(source: string): ReactNode[] {
  return splitInline(source).map((token, i) => {
    if (token.kind === 'bold') {
      return <strong key={i}>{token.text}</strong>;
    }
    if (token.kind === 'italic') {
      return <em key={i}>{token.text}</em>;
    }
    if (token.kind === 'code') {
      return <code key={i}>{token.text}</code>;
    }
    if (token.kind === 'link') {
      const isMail = token.url?.startsWith('mailto:');
      return (
        <a key={i} href={token.url} target={isMail ? undefined : '_blank'} rel="noopener noreferrer">
          {token.text}
        </a>
      );
    }
    if (token.kind === 'image') {
      return <img key={i} src={token.url} alt={token.text || 'Ảnh minh họa'} loading="lazy" />;
    }
    return <span key={i}>{token.text}</span>;
  });
}

// =========================================================
// PHẦN 3: LỌC HTML (chống chèn mã độc - XSS)
// =========================================================

// Các thẻ HTML được giữ lại
const ALLOWED_TAGS = new Set([
  'p', 'div', 'h2', 'h3', 'h4', 'blockquote', 'ul', 'ol', 'li', 'br',
  'strong', 'b', 'em', 'i', 'u', 's', 'del', 'strike', 'span', 'a', 'img', 'pre', 'code', 'font',
]);

// Các thẻ bị bỏ hẳn (kể cả nội dung bên trong)
const BLOCKED_TAGS = new Set(['script', 'style', 'iframe', 'object', 'embed', 'svg', 'math', 'template']);

// Cỡ chữ của thẻ <font size="1..7"> cũ
const FONT_SIZES: Record<string, string> = {
  '1': '10px', '2': '13px', '3': '16px', '4': '18px', '5': '24px', '6': '32px', '7': '48px',
};

const SAFE_COLOR = /^(#[0-9a-f]{3,8}|rgba?\([0-9.,%\s]+\)|[a-z]{3,20})$/i;
const SAFE_FONT_SIZE = /^(1[0-9]|2[0-9]|3[0-6])px$/; // từ 10px tới 36px

/** Chỉ giữ lại 3 kiểu định dạng an toàn: màu chữ, cỡ chữ, căn lề. */
function safeStyles(el: HTMLElement, tag: string) {
  const styles: string[] = [];

  let color = el.style.color;
  if (!color && tag === 'font') {
    color = el.getAttribute('color') || '';
  }
  if (color && SAFE_COLOR.test(color)) {
    styles.push('color:' + color);
  }

  let size = el.style.fontSize;
  if (!size && tag === 'font') {
    size = FONT_SIZES[el.getAttribute('size') || ''] || '';
  }
  if (size && SAFE_FONT_SIZE.test(size)) {
    styles.push('font-size:' + size);
  }

  const align = el.style.textAlign;
  if (align && ['left', 'center', 'right', 'justify'].includes(align)) {
    styles.push('text-align:' + align);
  }

  return styles.join(';');
}

/**
 * Lọc HTML: chép lần lượt từng thẻ sang một khung mới,
 * chỉ giữ thẻ / thuộc tính an toàn. Trả về chuỗi HTML sạch.
 */
export function sanitizeDescriptionHtml(source: string) {
  const parsed = new DOMParser().parseFromString(source, 'text/html');
  const output = document.createElement('div');

  function copy(node: Node, parent: HTMLElement) {
    // Chữ thường -> chép nguyên văn
    if (node.nodeType === Node.TEXT_NODE) {
      parent.appendChild(document.createTextNode(node.textContent || ''));
      return;
    }
    // Bỏ qua comment và các loại node khác
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return;
    }

    const el = node as HTMLElement;
    const tag = el.tagName.toLowerCase();

    if (BLOCKED_TAGS.has(tag)) {
      return;
    }

    // Thẻ lạ -> bỏ thẻ nhưng giữ nội dung bên trong
    if (!ALLOWED_TAGS.has(tag)) {
      for (const child of Array.from(el.childNodes)) {
        copy(child, parent);
      }
      return;
    }

    // Ảnh: chỉ giữ src (https) và alt
    if (tag === 'img') {
      const src = safeUrl(el.getAttribute('src') || '', true);
      if (!src) {
        return;
      }
      const image = document.createElement('img');
      image.src = src;
      image.alt = el.getAttribute('alt') || '';
      image.loading = 'lazy';
      parent.appendChild(image);
      return;
    }

    // Thẻ <font> cũ được đổi thành <span>
    const clean = document.createElement(tag === 'font' ? 'span' : tag);

    // Liên kết: chỉ giữ href an toàn và mở ở tab mới
    if (tag === 'a') {
      const href = safeUrl(el.getAttribute('href') || '');
      if (href) {
        clean.setAttribute('href', href);
        if (!href.startsWith('mailto:')) {
          clean.setAttribute('target', '_blank');
          clean.setAttribute('rel', 'noopener noreferrer');
        }
      }
    }

    const style = safeStyles(el, tag);
    if (style) {
      clean.setAttribute('style', style);
    }

    for (const child of Array.from(el.childNodes)) {
      copy(child, clean);
    }
    parent.appendChild(clean);
  }

  for (const child of Array.from(parsed.body.childNodes)) {
    copy(child, output);
  }
  return output.innerHTML;
}

// =========================================================
// PHẦN 4: CHUYỂN VĂN BẢN KIỂU MARKDOWN THÀNH CÁC KHỐI
// =========================================================

const HTML_TAG = /<\/?(?:p|div|h[1-6]|ul|ol|li|blockquote|strong|b|em|i|u|s|a|img|br|span|pre|code|font)\b/i;
const HEADING = /^(#{1,3})\s+(.+)$/;
const QUOTE = /^\s*>/;
const BULLET = /^\s*[-*+]\s+/;
const NUMBERED = /^\s*\d+[.)]\s+/;

/** Đọc từng dòng và gom thành tiêu đề, trích dẫn, danh sách, đoạn văn. */
function markdownBlocks(source: string): ReactNode[] {
  const lines = source.replace(/\r/g, '').split('\n');
  const blocks: ReactNode[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Dòng trống -> bỏ qua
    if (!line.trim()) {
      i++;
      continue;
    }

    // Tiêu đề: # / ## / ###  (đổi thành h2 / h3 / h4)
    const heading = line.match(HEADING);
    if (heading) {
      const level = heading[1].length + 1;
      const content = inlineNodes(heading[2]);
      if (level === 2) blocks.push(<h2 key={i}>{content}</h2>);
      else if (level === 3) blocks.push(<h3 key={i}>{content}</h3>);
      else blocks.push(<h4 key={i}>{content}</h4>);
      i++;
      continue;
    }

    // Trích dẫn: các dòng bắt đầu bằng >
    if (QUOTE.test(line)) {
      const quote: string[] = [];
      while (i < lines.length && QUOTE.test(lines[i])) {
        quote.push(lines[i].replace(/^\s*>\s?/, ''));
        i++;
      }
      blocks.push(
        <blockquote key={i}>
          {quote.map((text, n) => (
            <p key={n}>{inlineNodes(text)}</p>
          ))}
        </blockquote>,
      );
      continue;
    }

    // Danh sách gạch đầu dòng: - / * / +
    if (BULLET.test(line)) {
      const items: string[] = [];
      while (i < lines.length && BULLET.test(lines[i])) {
        items.push(lines[i].replace(BULLET, ''));
        i++;
      }
      blocks.push(
        <ul key={i}>
          {items.map((text, n) => (
            <li key={n}>{inlineNodes(text)}</li>
          ))}
        </ul>,
      );
      continue;
    }

    // Danh sách đánh số: 1. / 1)
    if (NUMBERED.test(line)) {
      const items: string[] = [];
      while (i < lines.length && NUMBERED.test(lines[i])) {
        items.push(lines[i].replace(NUMBERED, ''));
        i++;
      }
      blocks.push(
        <ol key={i}>
          {items.map((text, n) => (
            <li key={n}>{inlineNodes(text)}</li>
          ))}
        </ol>,
      );
      continue;
    }

    // Đoạn văn: gom các dòng liền nhau cho tới khi gặp dòng trống hoặc khối khác
    const paragraph = [line];
    i++;
    while (i < lines.length) {
      const next = lines[i];
      const startsNewBlock = HEADING.test(next) || QUOTE.test(next) || BULLET.test(next) || NUMBERED.test(next);
      if (!next.trim() || startsNewBlock) {
        break;
      }
      paragraph.push(next);
      i++;
    }
    blocks.push(
      <p key={i}>
        {paragraph.map((text, n) => (
          <Fragment key={n}>
            {n > 0 && <br />}
            {inlineNodes(text)}
          </Fragment>
        ))}
      </p>,
    );
  }

  return blocks;
}

/** Thành phần hiển thị mô tả tài liệu. */
export function DocumentDescription({ source }: { source?: string | null }) {
  // Mô tả dạng HTML
  if (source && HTML_TAG.test(source)) {
    return (
      <div
        className="document-description-content"
        dangerouslySetInnerHTML={{ __html: sanitizeDescriptionHtml(source) }}
      />
    );
  }

  // Mô tả dạng văn bản
  const blocks = markdownBlocks(source || '');
  return (
    <div className="document-description-content">
      {blocks.length > 0 ? blocks : <p className="muted">Chưa có mô tả chi tiết.</p>}
    </div>
  );
}
