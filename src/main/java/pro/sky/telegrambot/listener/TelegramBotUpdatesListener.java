package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private static final String REGEX = "([0-9.:\\s]{16})(\\s)([\\W+]+)";
    private final Pattern pattern = Pattern.compile(REGEX);

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;


    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message() == null) {
                return;
            }
            String text = update.message().text();
            Long chatId = update.message().chat().id();
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches()) {
                NotificationTask task = new NotificationTask();
                task.setMessage(matcher.group(3));
                task.setChatId(chatId);
                task.setExecDate(LocalDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER));
                notificationTaskRepository.save(task);
            } else if (text.equals("/start")) {
                telegramBot.execute(new SendMessage(chatId, "Hello, World!"));
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(fixedDelay = 5000)
    public void timer() {
        notificationTaskRepository.findAllByExecDateLessThan(LocalDateTime.now()).forEach(
                task -> {
                    telegramBot.execute(new SendMessage(task.getChatId(), task.getMessage()));
                    notificationTaskRepository.delete(task);
                }
        );


    }

}
