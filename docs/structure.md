# Project directory structure

Generated folder map. Empty extension folders contain .gitkeep.

```text
spring-boot-vscode/
|-- .vscode/
|-- backend/
|   |-- .mvn/
|   |   |-- wrapper/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/
|   |   |   |   |-- com/
|   |   |   |   |   |-- knowledge/
|   |   |   |   |   |   |-- app/
|   |   |   |   |   |   |   |-- common/
|   |   |   |   |   |   |   |   |-- constant/
|   |   |   |   |   |   |   |   |-- exception/
|   |   |   |   |   |   |   |   |-- response/
|   |   |   |   |   |   |   |   |-- util/
|   |   |   |   |   |   |   |   |-- validation/
|   |   |   |   |   |   |   |-- config/
|   |   |   |   |   |   |   |-- controller/
|   |   |   |   |   |   |   |   |-- admin/
|   |   |   |   |   |   |   |   |-- auth/
|   |   |   |   |   |   |   |   |-- category/
|   |   |   |   |   |   |   |   |-- comment/
|   |   |   |   |   |   |   |   |-- post/
|   |   |   |   |   |   |   |   |-- user/
|   |   |   |   |   |   |   |-- integration/
|   |   |   |   |   |   |   |   |-- ai/
|   |   |   |   |   |   |   |   |   |-- client/
|   |   |   |   |   |   |   |   |   |-- config/
|   |   |   |   |   |   |   |   |   |-- exception/
|   |   |   |   |   |   |   |   |   |-- payload/
|   |   |   |   |   |   |   |   |   |   |-- request/
|   |   |   |   |   |   |   |   |   |   |-- response/
|   |   |   |   |   |   |   |-- model/
|   |   |   |   |   |   |   |   |-- dto/
|   |   |   |   |   |   |   |   |   |-- request/
|   |   |   |   |   |   |   |   |   |   |-- auth/
|   |   |   |   |   |   |   |   |   |   |-- category/
|   |   |   |   |   |   |   |   |   |   |-- comment/
|   |   |   |   |   |   |   |   |   |   |-- post/
|   |   |   |   |   |   |   |   |   |   |-- user/
|   |   |   |   |   |   |   |   |   |-- response/
|   |   |   |   |   |   |   |   |   |   |-- auth/
|   |   |   |   |   |   |   |   |   |   |-- category/
|   |   |   |   |   |   |   |   |   |   |-- comment/
|   |   |   |   |   |   |   |   |   |   |-- post/
|   |   |   |   |   |   |   |   |   |   |-- user/
|   |   |   |   |   |   |   |   |-- entity/
|   |   |   |   |   |   |   |   |   |-- base/
|   |   |   |   |   |   |   |   |-- enums/
|   |   |   |   |   |   |   |   |-- mapper/
|   |   |   |   |   |   |   |-- repository/
|   |   |   |   |   |   |   |   |-- custom/
|   |   |   |   |   |   |   |   |-- impl/
|   |   |   |   |   |   |   |-- security/
|   |   |   |   |   |   |   |   |-- config/
|   |   |   |   |   |   |   |   |-- filter/
|   |   |   |   |   |   |   |   |-- handler/
|   |   |   |   |   |   |   |   |-- jwt/
|   |   |   |   |   |   |   |   |-- user/
|   |   |   |   |   |   |   |-- service/
|   |   |   |   |   |   |   |   |-- auth/
|   |   |   |   |   |   |   |   |-- category/
|   |   |   |   |   |   |   |   |-- comment/
|   |   |   |   |   |   |   |   |-- impl/
|   |   |   |   |   |   |   |   |   |-- auth/
|   |   |   |   |   |   |   |   |   |-- category/
|   |   |   |   |   |   |   |   |   |-- comment/
|   |   |   |   |   |   |   |   |   |-- post/
|   |   |   |   |   |   |   |   |   |-- user/
|   |   |   |   |   |   |   |   |-- post/
|   |   |   |   |   |   |   |   |-- user/
|   |   |   |-- resources/
|   |   |   |   |-- db/
|   |   |   |   |   |-- migration/
|   |   |   |   |-- static/
|   |   |   |   |-- templates/
|   |   |-- test/
|   |   |   |-- java/
|   |   |   |   |-- com/
|   |   |   |   |   |-- knowledge/
|   |   |   |   |   |   |-- app/
|   |   |   |-- resources/
|-- docs/
|   |-- database/
|-- frontend/
|   |-- public/
|   |-- src/
|   |   |-- app/
|   |   |   |-- layouts/
|   |   |   |-- providers/
|   |   |   |-- router/
|   |   |-- assets/
|   |   |   |-- fonts/
|   |   |   |-- icons/
|   |   |   |-- images/
|   |   |-- features/
|   |   |   |-- admin/
|   |   |   |   |-- api/
|   |   |   |   |-- components/
|   |   |   |   |-- hooks/
|   |   |   |   |-- pages/
|   |   |   |   |-- schemas/
|   |   |   |   |-- types/
|   |   |   |-- auth/
|   |   |   |   |-- api/
|   |   |   |   |-- components/
|   |   |   |   |-- hooks/
|   |   |   |   |-- pages/
|   |   |   |   |-- schemas/
|   |   |   |   |-- types/
|   |   |   |-- category/
|   |   |   |   |-- api/
|   |   |   |   |-- components/
|   |   |   |   |-- hooks/
|   |   |   |   |-- pages/
|   |   |   |   |-- schemas/
|   |   |   |   |-- types/
|   |   |   |-- comment/
|   |   |   |   |-- api/
|   |   |   |   |-- components/
|   |   |   |   |-- hooks/
|   |   |   |   |-- pages/
|   |   |   |   |-- schemas/
|   |   |   |   |-- types/
|   |   |   |-- post/
|   |   |   |   |-- api/
|   |   |   |   |-- components/
|   |   |   |   |-- hooks/
|   |   |   |   |-- pages/
|   |   |   |   |-- schemas/
|   |   |   |   |-- types/
|   |   |   |-- search/
|   |   |   |   |-- api/
|   |   |   |   |-- components/
|   |   |   |   |-- hooks/
|   |   |   |   |-- pages/
|   |   |   |   |-- schemas/
|   |   |   |   |-- types/
|   |   |   |-- user/
|   |   |   |   |-- api/
|   |   |   |   |-- components/
|   |   |   |   |-- hooks/
|   |   |   |   |-- pages/
|   |   |   |   |-- schemas/
|   |   |   |   |-- types/
|   |   |-- shared/
|   |   |   |-- api/
|   |   |   |-- components/
|   |   |   |   |-- feedback/
|   |   |   |   |-- ui/
|   |   |   |-- constants/
|   |   |   |-- hooks/
|   |   |   |-- lib/
|   |   |   |-- types/
|   |   |   |-- utils/
|   |   |-- styles/
```
