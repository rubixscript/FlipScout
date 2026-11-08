// Design tokens mirroring the Android app (Theme.kt Palette + Dimens): quiet monochrome
// canvas, soft cards, ink actions, one emerald for gains.

export const PALETTE = {
  emerald: '#34B27A',
  blue: '#5B9BF0',
  amber: '#F0A93B',
  violet: '#9B86F3',
  loss: '#D9443A',
};

export const STATUS_COLOR = {
  WATCHING: PALETTE.blue,
  BOUGHT: PALETTE.amber,
  LISTED: PALETTE.violet,
  SOLD: PALETTE.emerald,
};

export function makeTheme(dark) {
  return {
    dark,
    bg: dark ? '#0A0A0A' : '#FDFDFD',
    surface: dark ? '#161616' : '#F6F6F6',
    surfaceVar: dark ? '#212121' : '#EFEFEF',
    text: dark ? '#F2F2F2' : '#0B0B0B',
    muted: dark ? '#8E8E93' : '#7B7B7F',
    outline: dark ? '#2E2E2E' : '#DADADA',
    outlineVar: dark ? '#1F1F1F' : '#EAEAEA',
    ink: dark ? '#F5F5F5' : '#0B0B0B', // primary action fills
    onInk: dark ? '#0A0A0A' : '#FFFFFF',
    emerald: PALETTE.emerald,
    loss: PALETTE.loss,
  };
}

export const money = (n) =>
  '$' + (Number.isInteger(n) ? String(n) : Number(n || 0).toFixed(2));
