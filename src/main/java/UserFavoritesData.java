import io.vavr.control.Try;

public interface UserFavoritesData {
    Try<UserFavorite> retrieve(String userDiscordId, String name);
    Try<Void> store(UserFavorite userFavorite);
}
