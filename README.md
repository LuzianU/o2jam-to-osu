# o2jam-to-osu
Converts o2jam (ojn + ojm) charts to osu!. The resulting map will contain a single .ogg music file without hitsounds. 

# Requirements
- Java 8 or above
- Have the [audio-sample-mixer.exe](https://github.com/LuzianU/audio-sample-mixer/) next to the o2jam-to-osu.jar
  
# Usage
```java -jar o2jam-to-osu.jar -i <input_dir_or_single_ojn_file> -o <output_dir>``` 

Optional: 

```--od <od_float_value>``` overwrite default OD values

```--hp <hp_float_value>``` overwrite default HP value

```--server <server_name>``` will add this as the server tag of the converted difficulty names. Example: "O2Fantasia"<br>--> Difficulty name will be: [O2Jam] [O2Fantasia] [Level]

```--sv``` converted chart will have SV
