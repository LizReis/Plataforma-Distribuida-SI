# Plataforma Distribuída de Processamento Colaborativo de Tarefas

## 📌 Sobre o Projeto
Plataforma desenvolvida para a disciplina de Sistemas Distribuídos. O sistema realiza a orquestração e o processamento colaborativo de tarefas com suporte a balanceamento de carga (Round Robin), tolerância a falhas (Heartbeat e redistribuição) e consistência com Relógios Lógicos de Lamport.

## ⚙️ Arquitetura
- **Orquestrador Principal:** Coordena a rede e recebe requisições (Porta 5000 TCP).
- **Orquestrador Secundário (Backup):** Escuta o estado global via UDP Multicast (230.0.0.0:5005) e assume via Failover.
- **Workers:** Nós de execução que recebem as tarefas.
- **Clientes:** Submetem tarefas utilizando autenticação por Token.

## 🚀 Como Executar

### 1. Compilar os arquivos
Abra o terminal na pasta raiz do projeto (`/src`) e compile tudo:
`javac nodes/*.java services/*.java network/*.java model/*.java`

### 2. Iniciar os Orquestradores
Inicie o Backup primeiro (ficará escutando):
`java nodes.BackupOrchestrator`

Em outro terminal, inicie o Principal:
`java nodes.PrimaryOrchestrator`

### 3. Iniciar os Workers
Inicie ao menos 3 workers em terminais diferentes (passe a porta como argumento):
`java nodes.WorkerNode 5001`
`java nodes.WorkerNode 5002`
`java nodes.WorkerNode 5003`

### 4. Executar o Cliente
`java nodes.ClientNode`

## 🔐 Exemplo de Uso (Autenticação)
Usuários padrão disponíveis para teste na memória:
- **Login:** alexa | **Senha:** senha123
- **Login:** cliente1 | **Senha:** 1234
