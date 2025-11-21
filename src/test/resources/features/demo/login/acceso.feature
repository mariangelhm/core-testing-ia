Feature: Acceso demo
  @smoke
  Scenario: Login exitoso
    Given establezco el grupo "login"
    When escribo "usuario" en "input_user"
    When hago clic en "btn_ingresar"
    Then debería ver el texto "Bienvenido"
