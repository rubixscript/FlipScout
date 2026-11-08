const fs = require('fs');
const path = require('path');

// House pattern (RedScout / SeoScout): CRA copies public/ into build/, then this script
// (a) guarantees the extension shell files are present and (b) fixes CRA's absolute
// /static/... asset paths, which break inside a chrome-extension:// page.

const root = path.join(__dirname, '..');
const build = path.join(root, 'build');

// Copy the extension shell into build/
for (const name of ['manifest.json', 'background.js', 'content.js']) {
  const src = path.join(root, 'public', name);
  if (fs.existsSync(src)) {
    fs.copyFileSync(src, path.join(build, name));
    console.log(`✓ Copied ${name} to build folder`);
  }
}

// Copy icons folder into build/
const iconsSrc = path.join(root, 'public', 'icons');
const iconsDest = path.join(build, 'icons');
if (fs.existsSync(iconsSrc)) {
  fs.mkdirSync(iconsDest, { recursive: true });
  for (const icon of fs.readdirSync(iconsSrc)) {
    fs.copyFileSync(path.join(iconsSrc, icon), path.join(iconsDest, icon));
  }
  console.log('✓ Copied icons to build folder');
}

// Fix absolute -> relative asset paths in index.html for the extension origin
const indexHtmlPath = path.join(build, 'index.html');
if (fs.existsSync(indexHtmlPath)) {
  let indexHtml = fs.readFileSync(indexHtmlPath, 'utf8');
  indexHtml = indexHtml.replace(/href="\/static\//g, 'href="static/');
  indexHtml = indexHtml.replace(/src="\/static\//g, 'src="static/');
  fs.writeFileSync(indexHtmlPath, indexHtml);
  console.log('✓ Fixed paths in index.html for Chrome extension');

  // fullscreen.html ships the same bundle (SocialKat pattern): CRA only injects into
  // index.html, so graft its stylesheet/script tags onto the fullscreen page too.
  const fullscreenPath = path.join(build, 'fullscreen.html');
  if (fs.existsSync(fullscreenPath)) {
    const assets = [
      ...indexHtml.matchAll(/<link[^>]+href="static\/[^"]+"[^>]*>/g),
      ...indexHtml.matchAll(/<script[^>]+src="static\/[^"]+"[^>]*><\/script>/g),
    ].map((m) => m[0]);
    let fullscreenHtml = fs.readFileSync(fullscreenPath, 'utf8');
    fullscreenHtml = fullscreenHtml.replace('<!-- BUNDLE -->', assets.join('\n  '));
    fs.writeFileSync(fullscreenPath, fullscreenHtml);
    console.log(`✓ Injected ${assets.length} asset tag(s) into fullscreen.html`);
  }
}

console.log('\n✓ Build complete! Load the "build" folder in Chrome via chrome://extensions (Developer mode → Load unpacked).');
