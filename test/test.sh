#!/bin/bash
if [ "$1" == "clear" ]; then
  clear_mode=true
  echo "Running clear mode"
fi

if [ "$1" == "test" ]; then
  echo "Running test mode"
  echo "The most recently modified file is: $plugin_jar"
  exit 0
fi

if [ -z "$clear_mode" ] && [ "$1" != "nobuild" ] && [ "$1" != "norun" ]; then
  export JAVA_HOME="/mnt/fastssd/java/jdk-17.0.2/"
  cd ..
  ./gradlew --no-watch-fs  --stacktrace --info shadowJar || exit 1
  ./gradlew --no-watch-fs  --stacktrace  --info :test-plugin:build || exit 1
  cd test || exit 1
fi



plugin_jar=$(ls -tp "../build/libs" | grep -v '/$' | head -1)
plugin_jar=$(realpath "../build/libs/$plugin_jar")

tester_jar=$(realpath "../test-plugin/build/libs/test-plugin.jar")

VERSIONS_SETTINGS="settings"
for file in "$VERSIONS_SETTINGS"/*; do
  echo "Processing $file"
  if [ -f "$file" ]; then
    source "$file"
    if [[ -z "$JAVA_RUN" ]]; then
      echo "No JAVA_RUN defined in $file"
      exit 1
    fi

    if [[ -z "$SOURCE" ]]; then
      echo "No SOURCE defined in $file"
      exit 1
    fi
    file_name=$(basename "$file")
    folder="${file_name%.*}"
    if [ -n "$clear_mode" ]; then
      echo "Clearing $folder"
      rm -rf "$folder"
    else
      mkdir -p "$folder"
      for server_file in "server-settings"/*; do
        echo "$server_file"
        cp -rf "$server_file" "$folder"
      done

      echo "Installing $SOURCE"
      if [ -e "$folder"/server.jar ]; then
        echo "Found existing server.jar, not downloading"
      else
        wget -O "$folder"/server.jar "$SOURCE" || exit 1
      fi
      mkdir -p "$folder/plugins"
      ln -sf "$tester_jar" "$folder/plugins/test-plugin.jar" || exit 1
      ln -sf "$plugin_jar" "$folder/plugins/sig.jar" || exit 1
      cd "$folder" || exit 1
      if [ "$1" != "norun" ]; then
        echo "2" > results.txt
        "$JAVA_RUN" -Xmx1024M -Xms1024M -jar server.jar nogui || exit 1
        if [ "$(cat results.txt)" == "0" ]; then
          echo "$folder passed successfully"
        else
          echo "Error. $folder failed with exit code: $(cat results.txt)"
          exit 1
        fi
        cd ..
      fi
    fi
  fi
done
