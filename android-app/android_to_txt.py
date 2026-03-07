import os
from pathlib import Path

def extract_android_code(project_path, output_file):
    # Extensiones esenciales para entender un proyecto Android
    valid_extensions = {'.kt', '.kts', '.xml', '.gradle'} 
    
    # Carpetas que generan mucho ruido y deben ignorarse
    exclude_dirs = {
        'build', '.gradle', '.idea', 'captures', 
        'bin', 'obj', '.git', 'test', 'androidTest'
    }

    project_root = Path(project_path)
    
    with open(output_file, 'w', encoding='utf-8') as outfile:
        for root, dirs, files in os.walk(project_root):
            # Filtrar carpetas de construcción y sistema
            dirs[:] = [d for d in dirs if d not in exclude_dirs and not d.startswith('.')]

            for file in files:
                file_path = Path(root) / file
                
                # Filtrar por extensión
                if file_path.suffix in valid_extensions:
                    # Ignorar archivos binarios o pesados que se colaran
                    if "res/drawable" in str(file_path) or "res/mipmap" in str(file_path):
                        continue
                        
                    relative_path = file_path.relative_to(project_root)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as infile:
                            content = infile.read()
                            
                            # Separador visual claro
                            outfile.write(f"\n{'='*80}\n")
                            outfile.write(f"UBICACIÓN: {relative_path}\n")
                            outfile.write(f"{'='*80}\n\n")
                            
                            outfile.write(content)
                            outfile.write("\n\n")
                            
                        print(f"✔️ Incluido: {relative_path}")
                    except Exception as e:
                        print(f"⚠️ No se pudo leer {relative_path}: {e}")

if __name__ == "__main__":
    # Configuración de rutas
    PATH_DEL_PROYECTO = "." 
    ARCHIVO_SALIDA = "contexto_android_kotlin.txt"
    
    print(f"🤖 Procesando proyecto Android en: {Path(PATH_DEL_PROYECTO).absolute()}")
    extract_android_code(PATH_DEL_PROYECTO, ARCHIVO_SALIDA)
    print(f"\n✅ Proceso terminado. Archivo generado: {ARCHIVO_SALIDA}")