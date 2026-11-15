# Esperaí — app de horários de ônibus intermunicipal

App Android (Kotlin + Jetpack Compose + banco Room/SQLite local). O mapa é o Google Maps, aberto por link.

## Duas versões do mesmo código
- **Esperaí Admin** (`app.esperai.admin`): só para você. Tem o Cadastro (linhas, paradas, ordem, horários) e exporta o `dados.json`.
- **Esperaí** (`app.esperai`): para os usuários. Só consulta e, ao abrir, baixa sozinha o `dados.json` do site se houver versão nova.

## Como gerar os APKs (sem instalar nada)
1. Crie um repositório **público** no GitHub (ex.: `esperai`) e envie todo o conteúdo desta pasta (inclusive a pasta oculta `.github`).
2. No repositório: Settings > Secrets and variables > Actions > New repository secret. Crie dois:
   - `KEYSTORE_BASE64`: todo o texto do arquivo `chave-base64.txt`
   - `KEYSTORE_PASSWORD`: a senha que está em `chave-senha.txt`
3. Aba Actions > "Compilar APK" > Run workflow. Ao terminar, baixe o artefato **apks**: ele traz `app-admin-release.apk` e `app-usuario-release.apk`.

## Publicar o site e as atualizações (GitHub Pages)
1. Settings > Pages > Deploy from a branch > `main` / pasta `/docs`.
2. No app Admin: Cadastro > Exportar dados. Salve como `dados.json` e coloque na pasta `docs/` do repositório.
3. O app do usuário procura `https://SEU-USUARIO.github.io/esperai/dados.json` (o endereço é montado automaticamente pelo nome do repositório).
4. Para o app avisar que há APK novo: aumente `versionCode` em `app/build.gradle.kts`, recompile e exporte o `dados.json` de novo.

## Regras importantes
- **Nunca perca a chave** (`esperai.jks`). Sem ela, não dá para atualizar o app por cima da versão instalada.
- Cada APK novo precisa de `versionCode` maior que o anterior.
- Se mudar a estrutura do banco no futuro, será preciso criar uma migração do Room.
