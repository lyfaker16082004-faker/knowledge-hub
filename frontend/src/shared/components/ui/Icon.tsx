/*
 * Danh sách icon dùng trong ứng dụng.
 * Mỗi icon là một đường vẽ SVG (thuộc tính d của thẻ <path>) trên khung 24x24.
 */
const paths: Record<string, string> = {
 upload: 'M12 16V3 M7 8l5-5 5 5 M4 16v5h16v-5',
 institution: 'M3 21h18 M5 21V8h14v13 M3 8l9-5 9 5 M8 11v6 M12 11v6 M16 11v6',
 download: 'M12 3v12 M7 10l5 5 5-5 M4 16v5h16v-5',
 bookmark: 'M6 3h12v18l-6-4-6 4V3',
 gift: 'M3 8h18v4H3z M5 12v9h14v-9 M12 8v13 M12 8H8a3 3 0 1 1 3-3l1 3Zm0 0h4a3 3 0 1 0-3-3l-1 3Z',
 code: 'm8 6-6 6 6 6 M16 6l6 6-6 6 M14 3l-4 18',
 database: 'M20 5c0 2-4 3-8 3S4 7 4 5s4-3 8-3 8 1 8 3Z M4 5v14c0 2 4 3 8 3s8-1 8-3V5 M4 12c0 2 4 3 8 3s8-1 8-3',
 spark: 'm12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7',
 menu: 'M4 6h16 M4 12h16 M4 18h16',
 clock: 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0 M12 7v5l3 2',

 bell: 'M6 9a6 6 0 1 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9 M10 20a2 2 0 0 0 4 0',
 camera: 'M4 8h3l2-3h6l2 3h3v12H4z M12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
 home: 'M3 11 12 3l9 8 M5 9.5V21h5v-6h4v6h5V9.5',
 settings: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z',
 help: 'M4 3h16v18H4z M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.5v.7 M12 17h.01',
 book: 'M4 4h6a3 3 0 0 1 3 3v14a4 4 0 0 0-4-3H4V4Zm16 0h-4a3 3 0 0 0-3 3m0 14a4 4 0 0 1 4-3h3V4',
 grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
 user: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2 M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
 users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M22 21v-2a4 4 0 0 0-3-3.87 M17 3.13a4 4 0 0 1 0 7.75',
 shield: 'M12 3 3 7v5c0 5 9 10 9 10s9-5 9-10V7L12 3 M8 12l3 3 5-6',
 arrow: 'M5 12h14 M13 6l6 6-6 6', logout: 'M9 21H4V3h5 M9 12h12 M16 7l5 5-5 5',
 plus: 'M12 5v14 M5 12h14', search: 'M21 21l-5-5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
 lock: 'M5 10h14v12H5z M8 10V6a4 4 0 0 1 8 0v4 M12 15v3',
 check: 'M5 12l4 4L19 6', eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12 M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
 close: 'M6 6l12 12 M6 18 18 6', mail: 'M3 5h18v14H3z M3 5l9 8 9-8',
 edit: 'm16 3 5 5-12 12-6 1 1-6L16 3 M14 5l5 5',
 trash: 'M3 6h18 M8 6V4h8v2 M19 6l-1 15H6L5 6 M10 11v6 M14 11v6',
 chevronLeft: 'm15 18-6-6 6-6', chevronRight: 'm9 18 6-6-6-6',
 refresh: 'M20 7v5h-5 M4 17v-5h5 M5.6 9A7 7 0 0 1 18.3 6L20 8 M4 16l1.7 2A7 7 0 0 0 18.4 15',
 eyeOff: 'M3 3l18 18 M9.9 5.2A11 11 0 0 1 12 5c6 0 10 7 10 7a19 19 0 0 1-3.2 3.9 M6.2 6.2A20 20 0 0 0 2 12s4 7 10 7a12 12 0 0 0 5.3-1.3 M10 10a3 3 0 0 0 4 4',
 // community icons
 heart: 'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8Z',
 message: 'M21 12a8 8 0 0 1-11.6 7.1L4 20.5l1.4-4.9A8 8 0 1 1 21 12Z',
 share: 'M18 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z M6 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z M18 22a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z M8.6 13.5l6.8 4 M15.4 6.5l-6.8 4',
 more: 'M5 12h.01 M12 12h.01 M19 12h.01',
 up: 'm6 15 6-6 6 6', down: 'm6 9 6 6 6-6',
 reply: 'M9 17 4 12l5-5 M4 12h11a5 5 0 0 1 5 5v2',
 trending: 'M3 17l6-6 4 4 8-8 M15 7h6v6',
 hash: 'M4 9h16 M4 15h16 M10 3 8 21 M16 3l-2 18',
 question: 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0 M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3 M12 17h.01',
 // topic icons
 coffee: 'M17 8h1a4 4 0 1 1 0 8h-1 M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V8Z M6 2v2 M10 2v2 M14 2v2',
 layout: 'M3 4h18v16H3z M3 9h18 M9 9v11',
 bulb: 'M9 18h6 M10 22h4 M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2Z',
 branch: 'M6 3v12 M18 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z M18 9a9 9 0 0 1-9 9',
 network: 'M9 3h6v5H9z M12 8v4 M5 12h14 M5 12v4 M12 12v4 M19 12v4 M3 16h4v5H3z M10 16h4v5h-4z M17 16h4v5h-4z',
 terminal: 'M3 4h18v16H3z M7 9l3 3-3 3 M13 15h4',
 sigma: 'M18 4H6l6 8-6 8h12',
 cloud: 'M17.5 19H7a5 5 0 1 1 1.1-9.9A6 6 0 0 1 19.5 11a4 4 0 0 1-2 8Z',
 cart: 'M3 3h2l2.4 12.4a2 2 0 0 0 2 1.6h8.2a2 2 0 0 0 2-1.6L21 7H6 M10 21h.01 M18 21h.01',
 python: 'M12 3c-4 0-4 1.5-4 3v2h4.5 M8 6.5H6c-2 0-3 1.5-3 4.5s1 4.5 3 4.5h2 M12 21c4 0 4-1.5 4-3v-2h-4.5 M16 17.5h2c2 0 3-1.5 3-4.5s-1-4.5-3-4.5h-2 M8 8v3a2 2 0 0 0 2 2h4a2 2 0 0 1 2 2v1 M10 5.5h.01 M14 18.5h.01',
};
/** Icon được vẽ nhỏ hơn kích thước yêu cầu khoảng 15% để giao diện gọn hơn. */
const ICON_SCALE = 0.85;

/**
 * Hiển thị một icon SVG theo tên, ví dụ: <Icon name="book" size={20} />
 * Nếu tên không có trong danh sách thì dùng icon "book".
 */
export default function Icon({ name, size = 20 }: { name: string; size?: number }) {
  const px = Math.max(12, Math.round(size * ICON_SCALE));
  const path = paths[name] || paths.book;

  return (
    <svg
      width={px}
      height={px}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={path} />
    </svg>
  );
}
