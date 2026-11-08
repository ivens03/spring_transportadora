# 0001. Organizar a API do RotaCerta em módulos de negócio com Spring Boot e persistência relacional

* **Status:** Aceito
* **Data:** 2026-11-07

## Contexto
O RotaCerta reúne operações de usuários, autenticação, cargas, encomendas, rotas e rastreamento de entregas. O backend precisa centralizar regras de negócio e autorização para clientes web e Android, manter dados relacionais consistentes e permitir evolução independente dos módulos funcionais sem separar necessariamente o sistema em vários serviços. Além disso, precisamos garantir que a comunicação entre a API e o banco de dados esteja sempre bem estruturada evitando dessa forma qualquer tipo de falha na segurança ou comunicação com o servidor VPS da oracle.

## Decisão
Adotar **Java com Spring Boot** para uma **API organizada por módulos de domínio**, cada um com camadas próprias de controller, DTO, model, repository e service. Utilizar **Spring Security com JWT** para autenticação e autorização, **Spring Data JPA** para acesso a dados, **PostgreSQL** como banco relacional e **Flyway** para versionamento do esquema. Manter documentação da API com **OpenAPI/Swagger** e testes com ferramentas do ecossistema Spring, incluindo **Testcontainers**.

## Alternativas Consideradas
* **Arquitetura separada apenas por camadas globais:** Centralizaria todos os controllers e services, mas reduziria a proximidade entre arquivos relacionados a um mesmo domínio.
* **Microsserviços independentes:** Permitiriam implantação separada de domínios com maior controle de cada um deles e uma escalabilidade superior ao de um monólito, mas acrescentariam comunicação distribuída e complexidade operacional para o escopo observado.
* **Banco não relacional como persistência principal (MongoDB):** Poderia atender a alguns dados flexíveis, mas demandaria uma modelagem diferente das entidades e relações atualmente tratadas por JPA e PostgreSQL.

## Consequências
* **Positivas:**
  * Organização das responsabilidades por domínio e por camada interna.
  * Regras de negócio e autorização concentradas na API, compartilhadas pelo frontend e pelo Android.
  * Persistência relacional com migrações versionadas e possibilidade de testes de integração em ambiente controlado.
  * Possuimos mais afinidade com a linguagem de backend JAVA.
* **Negativas:**
  * Necessidade de manter configurações, migrações e dependências de banco de dados.
  * Maior acoplamento operacional entre módulos implantados na mesma aplicação.
  * Exigência de cuidados com emissão e validação de JWT e evolução dos contratos consumidos pelos clientes.
  * Maior consumo de recursos: aplicações Java podem apresentar consumo elevado de memória RAM e maior tempo de inicialização da JVM.
  * Maior complexidade de escalabilidade horizontal.

