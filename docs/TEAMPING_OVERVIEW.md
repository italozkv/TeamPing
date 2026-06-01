# TeamPing

TeamPing é um mod de qualidade de vida para Minecraft 1.21.1 com NeoForge, pensado para coop survival e servidores dedicados. A ideia central é permitir comunicação visual rápida entre jogadores do mesmo time, usando pings no mundo, menus radiais e ícones personalizáveis.

## Visão Geral

O mod combina três camadas:

1. Organização de times.
2. Sistema de ping cooperativo.
3. Interface radial para seleção rápida de ações e ícones.

Tudo que importa de validação roda no servidor. O client só captura entrada, desenha a interface e mostra os pings recebidos.

## Estado Atual Do Projeto

### Times

- Comando para entrar em times: azul, vermelho, verde e amarelo.
- Comando para sair do time.
- Persistência do time do jogador via dados salvos do próprio player.
- Sincronização pensada para multiplayer e servidor dedicado.

### Ping

- Ping por tecla `G`.
- Fluxo hold-to-open para abrir a roda radial.
- Fluxo de release para confirmar o ping.
- Validação no servidor antes de espalhar o ping.
- Distância máxima configurável.
- Cooldown configurável.
- Limite configurável de pings ativos por jogador.
- Suporte a cross-dimension opcional via config.

### Tipos de alvo

- Bloco.
- Item dropado.
- Mob hostil.
- Mob pacífico.

O sistema já consegue seguir entidades móveis no client enquanto o ping existir.

### Renderização

- Marcador no mundo.
- Distância exibida acima do ping.
- Cor do ping baseada no time.
- Ícone do ping baseado no tipo de alvo.
- Fallback visual para quando a entidade some ou descarrega.

### Menu Radial

- Menu radial em hold-to-open.
- Segmentos para DANGER, RESOURCE, COMBAT, LOOT, BASE e LOCATION.
- Seleção de time dentro da própria roda.
- Seção de extras com ícones personalizados.
- Seleção salva no client.

### Ícones

- Ícones principais convertidos para PNG e integrados no pacote do mod.
- Extras organizados em `textures/gui/pings/extras`.
- Estrutura preparada para expansão futura.

### Networking

- Payload cliente para servidor para criar ping.
- Payload servidor para cliente para exibir ping.
- Fluxo mantido server-authoritative.

### Build

- O projeto compila com sucesso com `.\gradlew.bat build`.
- O jar é gerado em `build/libs/teamping-1.0.0.jar`.

## Estrutura Funcional

### Client Only

- Captura de tecla.
- Abertura e fechamento da roda radial.
- Leitura da posição do mouse.
- Renderização do menu e dos pings na HUD.
- Persistência local da escolha de ícone extra.

### Common / Server

- Registro do mod.
- Configuração server-side.
- Comandos de time.
- Validação de ping.
- Persistência do time do jogador.
- Distribuição do ping para teammates.

## Arquivos-Chave

- `src/main/java/dev/ithalo/teamping/TeamPing.java`
- `src/main/java/dev/ithalo/teamping/config/TeamPingConfig.java`
- `src/main/java/dev/ithalo/teamping/client/PingWheelInputHandler.java`
- `src/main/java/dev/ithalo/teamping/client/PingWheelRenderer.java`
- `src/main/java/dev/ithalo/teamping/client/ClientPingRenderer.java`
- `src/main/java/dev/ithalo/teamping/server/ServerPingManager.java`
- `src/main/java/dev/ithalo/teamping/team/PlayerTeamData.java`
- `src/main/java/dev/ithalo/teamping/network/*`
- `src/main/java/dev/ithalo/teamping/ping/*`

## Ícones Disponíveis

### Principais

- `danger`
- `resource`
- `combat`
- `loot`
- `base`
- `location`
- `enemy`
- `animal`

### Extras

- `bed`
- `check_mark`
- `death_location`
- `eye`
- `flag`
- `food`
- `mine`
- `player`
- `question_mark`
- `shield`
- `users_group`

## Ideias Futuras

### Curto Prazo

- Organizar os extras em páginas ou categorias dentro do radial.
- Permitir troca de pacote visual por jogador.
- Adicionar tooltip curto para cada ícone extra.
- Melhorar a indicação de qual ícone extra está selecionado.

### Médio Prazo

- Criar um sistema de presets de ping por time.
- Permitir que o time defina um conjunto de ícones padrão.
- Sincronizar presets entre jogadores do mesmo time.
- Adicionar suporte a mais tipos de alvo, como estruturas e pontos de interesse.

### Longo Prazo

- Menu radial com múltiplos níveis.
- Sistema de favoritos para ícones mais usados.
- Configuração por mundo ou por servidor.
- Integração com um painel de gerenciamento in-game.
- Sons diferentes por tipo de ping.
- Animação mais rica de entrada e saída do marcador.

## Observações Técnicas

- O mod depende de renderização client-side para a interface, mas não deixa o client decidir quem recebe ping.
- O servidor continua sendo a autoridade sobre validação, cooldown e distribuição.
- Os ícones devem continuar em PNG para o runtime do Minecraft; os SVGs são úteis como fonte de edição.
- O fluxo atual foi pensado para ser seguro em servidor dedicado e em coop local entre amigos.

## Próximo Passo Recomendado

O próximo passo natural é transformar a área de extras em um menu radial próprio, com navegação mais elegante e categorias para não lotar a interface.
