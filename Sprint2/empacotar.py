"""Build deliverable archives without credentials, caches or machine-specific files."""
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
import hashlib
import shutil

root = Path(__file__).resolve().parent
output = root / 'entrega'
output.mkdir(exist_ok=True)
ignored = {'build', 'target', '.gradle', '.idea', '.git', '__pycache__'}

def pack(folder, name, include_apk=False):
    with ZipFile(output / name, 'w', ZIP_DEFLATED) as archive:
        for file in sorted((root / folder).rglob('*')):
            if not file.is_file():
                continue
            relative = file.relative_to(root / folder)
            if ignored.intersection(relative.parts) or file.name in {'.env', 'local.properties'} or file.suffix in {'.log', '.jks', '.keystore'}:
                continue
            if file.name in {'mvnw', 'gradlew'}:
                info = ZipInfo.from_file(file, str(Path(folder) / relative))
                info.create_system = 3
                info.external_attr = 0o100755 << 16
                info.compress_type = ZIP_DEFLATED
                archive.writestr(info, file.read_bytes())
            else:
                archive.write(file, Path(folder) / relative)
        if include_apk:
            archive.write(root / 'android/app/build/outputs/apk/debug/app-debug.apk', 'android/VoaAguia-Sprint2.apk')

apk = root / 'android/app/build/outputs/apk/debug/app-debug.apk'
if not apk.is_file():
    raise SystemExit('Compile o APK antes de empacotar.')
pack('backend', 'VoaAguia-Backend-Sprint2.zip')
pack('android', 'VoaAguia-Android-Sprint2.zip', include_apk=True)
shutil.copy2(apk, output / 'VoaAguia-Sprint2.apk')
for name in ['Apresentacao-Sprint2.pptx', 'Apresentacao-Sprint2.pdf', 'Documentacao-Sprint2.pdf', 'ENDPOINTS.md', 'openapi.json', 'VALIDACAO.md', 'ROTEIRO-GRAVACAO.md']:
    source = root / 'docs' / name
    if source.is_file():
        shutil.copy2(source, output / name)
shutil.copy2(root / 'README.md', output / 'LEIA-ME.md')
checksums=[]
for file in sorted(output.iterdir()):
    if file.is_file() and file.name != 'SHA256SUMS.txt':
        checksums.append(hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + file.name)
(output / 'SHA256SUMS.txt').write_text('\n'.join(checksums)+'\n', encoding='utf-8')
print('Entrega gerada em', output)
