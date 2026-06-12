# Mi Carrito de Compra 🛒

Una aplicación móvil moderna, intuitiva y robusta para la gestión de listas de la compra, desarrollada para la plataforma Android utilizando tecnologías de vanguardia recomendadas por Google.

La aplicación permite organizar tus compras diarias mediante listas personalizadas, gestionar productos con sus precios y cantidades, categorizarlos con etiquetas coloridas e interactivas, y mantener un historial detallado de tus gastos. Cuenta además con un potente sistema local de copia de seguridad que no compromete tu privacidad.

Se incluye en este repositorio el fichero apk para su descarga directa e instalación en el móvil sin necesidad de compilar, ni de instalar android studio ni ná de ná :)

---

## ✨ Características Principales

### 📋 Gestión de Listas de la Compra
*   **Múltiples listas simultáneas**: Crea listas individuales para diferentes establecimientos, ocasiones o categorías (ej. "Supermercado", "Frutería", "Barbacoa").
*   **Totalización en tiempo real**: Visualiza el coste total estimado y real de cada lista a medida que añades e introduces artículos al carrito.
*   **Interactividad ágil**: Marca productos completados, incrementa cantidades o elimina elementos rápidamente desde una interfaz fluida.

### 🏷️ Categorías y Etiquetas Personalizadas
*   Organiza tus compras utilizando etiquetas coloridas acompañadas de divertidos emoticonos representativos (ej. 🍞 Panadería, 🥛 Lácteos, 🥦 Verduras).
*   Crea, edita o elimina tus propias etiquetas para adaptar la aplicación a tus hábitos de consumo específicos.

### 💰 Control de Precios e Historial de Gastos
*   Mantén un seguimiento preciso de tus finanzas personales asociando cantidades estimadas y precios unitarios a tus productos habituales.
*   **Historial de Compras terminado**: Cuando completas una compra, el registro se almacena en el historial organizando los importes finales de forma detallada.

### 💾 Copia de Seguridad y Portabilidad (Directo a Archivos)
*   **Exportación a Archivos**:
    *   **Formato JSON**: Exporta una copia de seguridad idéntica de la base de datos (con tus listas, productos, etiquetas personalizadas e historial completo) seleccionando la ruta y el nombre del archivo en tu dispositivo.
    *   **Formato CSV**: Exporta tus listas vigentes a un archivo delimitado por comas (`.csv`), ideal para abrir y analizar en herramientas de hojas de cálculo como Microsoft Excel, Google Sheets o LibreOffice.
*   **Importación desde Archivos**:
    *   Carga directamente tus archivos `.json` de copia de seguridad o `.csv` desde el explorador del dispositivo.
    *   **Estrategia de resolución de conflictos**: Decide si deseas **fusionar con los datos actuales** o si prefieres **sobrescribir completamente** la base de datos para una restauración limpia.

---

## 🛠️ Tecnologías y Arquitectura

La aplicación ha sido desarrollada bajo las mejores prácticas de la ingeniería de software móvil para Android:

*   **Lenguaje**: **Kotlin** (100%), garantizando un código conciso y seguro ante nulos.
*   **Interfaz de Usuario (UI)**: **Jetpack Compose**, permitiendo un desarrollo composable moderno y reactivo con transiciones pulidas y adaptadas a **Material Design 3 (M3)**.
*   **Base de Datos Local**: **Room Database** (SQLite con interfaz segura sobre Kotlin), ofreciendo almacenamiento local ultrarrápido y sin dependencia de servicios externos en la nube para garantizar absoluta privacidad.
*   **Gestión de Estado**: Patrón de arquitectura **MVVM (Model-View-ViewModel)** mediante `ViewModel` y flujos asíncronos fríos/calientes con `StateFlow` y programación concurrente con **Coroutines**.
*   **Inyección y Portabilidad**: Integración nativa mediante `Activity Results contract` (`OpenDocument` y `CreateDocument`) para interactuar de forma segura con el sistema de archivos de Android sin requerir permisos invasivos de almacenamiento global.

---

## 📂 Estructura del Proyecto

A continuación, se detalla la organización de los paquetes principales en el código fuente:

```text
/app/src/main/java/com/example/
├── data/
│   ├── AppDatabase.kt       # Configuración y migración de la base de datos Room
│   ├── Entities.kt          # Definición de tablas de base de datos (List, Product, Tag, History)
│   └── ShoppingDao.kt       # Consultas SQL seguras y operaciones de transacción (Query, Insert, Delete)
├── repository/
│   └── ShoppingRepository.kt# Capa de abstracción y mediación de datos
├── ui/
│   ├── ShoppingApp.kt       # Pantallas principales de interfaz gráfica (Listas, Creación, Backup Dialog)
│   └── Theme.kt             # Configuración de colores globales, fuentes y paleta Material Design 3
├── utils/
│   └── BackupHelper.kt      # Lógicas de serialización/deserialización para JSON y manipulación de cadenas CSV
└── viewmodel/
    └── ShoppingViewModel.kt  # Gestión del estado de la interfaz gráfica y comunicación con el repositorio
```

---

## 🚀 Cómo Empezar

### Requisitos Previos
*   **Android Studio** Ladybug (o superior).
*   **JDK 17** o posterior.
*   Dispositivo físico o emulador con **Android 7.0 (API nivel 24)** o superior.

### Instrucciones de Compilación

1.  **Clonar el repositorio**:
    ```bash
    git clone https://github.com/fjbernal/carrito-compras.git
    cd carrito-compras
    ```

2.  **Abrir el proyecto en Android Studio**:
    *   Inicia Android Studio, pulsa en `Open` y selecciona la carpeta raíz del proyecto.

3.  **Compilar y Ejecutar**:
    *   Espera a que Gradle sincronice todas las dependencias.
    *   Selecciona tu dispositivo o emulador objetivo y pulsa sobre el botón verde de ejecución (**Run** / `Shift + F10`).

---

## 🛡️ Privacidad y Seguridad

Tu información te pertenece. **Mi Carrito de Compra** funciona de manera **100% desconectada de Internet (Offline-First)**. No hay telemetría oculta, no se recopilan datos de tus compras ni se envían listados de productos a servidores de terceros. Las copias de seguridad son archivos locales que tú controlas de principio a fin.
