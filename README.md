<div align="center">
  <img src=https://github.com/RichtXO/Lil-Mo/assets/22281588/0dd25da7-b9a1-4cce-8935-ca9e1e696a09 style="width:200px;">
  <h3 align="center">Lil Mo</h3>
  Another Discord Music Bot in Java

</div>


## Getting Started
It's easy to get started via Docker containers! Also need a valid Discord App Token and Spotify Premium Token!

[Discord Invite Link](https://discord.com/oauth2/authorize?client_id=1216105100448108584&permissions=3145728&scope=bot)

### Prerequisites
* [Docker Desktop](https://www.docker.com/products/docker-desktop/)
* [Discord Token](https://discord.com/developers/)
* [Spotify Token](https://developer.spotify.com/)

### Installation
1. Get a new Discord Application Token from [Discord Developer Portal](https://discord.com/developers/)
2. Get a Spotify App Client ID and Secret Tokens from [Spotify Developer Dashboard](https://developer.spotify.com/)
3. Clone the repo and change directory
   ```sh
   git clone https://github.com/RichtXO/Lil-Mo.git
   cd Lil-Mo
   ```
4. Copy these tokens and generate a new `.env` to hold them!
   ```sh
   TOKEN={discord_token}
   SPOTIFY_CLIENT_ID={spotify_client_id}
   SPOTIFY_SECRET={spotify_client_secret}
   YT_TOKEN={yt_poToken}
   YT_VISITOR={yt_visiterData}
   ```
5. Run Lil Mo!
   ```sh
   docker compose up --build -d
   ```

## Libraries Used
* [Discord4J](https://github.com/Discord4J/Discord4J)
* [LavaPlayer](https://github.com/lavalink-devs/lavaplayer)
* [Spotify Web API Java](https://github.com/spotify-web-api-java/spotify-web-api-java)

