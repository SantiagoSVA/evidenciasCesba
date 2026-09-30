# Evidencias Fotográficas

Proyecto Android en Java para registrar evidencias con una fotografía de resolución completa, título, descripción, fecha y hora de captura.

## Abrir

1. Descomprime el ZIP, si recibiste el proyecto comprimido.
2. En Android Studio selecciona **Open** y elige la carpeta `EvidenciasFotograficas`.
3. Espera la sincronización de Gradle. Se requiere JDK 17 o superior y Android SDK 36. Android Studio puede instalar el SDK y las dependencias si faltan.
4. Ejecuta `app` en un teléfono o emulador con una aplicación de cámara disponible.

## Uso

1. **Tomar fotografía** abre la cámara del dispositivo. **Tomar otra fotografía** reemplaza la foto aún sin registrar, pero conserva la anterior si cancelas la nueva captura.
2. Escribe título y descripción y pulsa **Guardar evidencia**. Cada evidencia registrada contiene una foto. El contador refleja el número de evidencias guardadas.
3. **Eliminar foto actual** descarta solo la captura aún no registrada.
4. **Guardar fotografía en el dispositivo** permite elegir una ubicación para exportar el JPEG actual.
5. En la pantalla principal, pulsa **Compartir evidencias** o **Generar ficha de evidencias** y elige una evidencia guardada. La primera opción abre el menú para compartir la foto con su título, descripción y fecha. La segunda permite escoger dónde guardar una ficha PDF que incluye todos esos datos y la imagen.
6. En **Galería de evidencias**, abre una ficha para ver sus datos, guardar el JPEG, compartirla, crear su PDF o eliminar la evidencia registrada.

Las evidencias se guardan en el almacenamiento interno privado de la aplicación. Desinstalarla elimina estos datos; exporta las fotografías o fichas PDF que quieras conservar fuera de la app. La ficha PDF incluye la foto y sus datos. No se requiere permiso de cámara porque se invoca la aplicación de cámara instalada; tampoco se requiere permiso de almacenamiento para exportar mediante el selector de documentos.

El código original se adaptó para guardar fotografías completas: `ActivityResultContracts.TakePicture` escribe en una URI de `FileProvider`, en vez de usar la miniatura `data` de `ACTION_IMAGE_CAPTURE`.
