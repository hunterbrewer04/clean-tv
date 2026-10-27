# Set up Clean TV with an AI assistant

Copy the prompt below into ChatGPT, Claude, Gemini, or a coding agent like Claude Code or Codex. Fill in the three lines under `<my_setup>`, or leave them and it'll ask you.

- **Chat assistants** can't reach your TV. They give you one command at a time, you run it, and you paste back what it printed.
- **Agents with a terminal** run the read-only commands themselves and ask you before anything that changes the TV.

If your assistant can't open links, paste the contents of [GUIDE.md](GUIDE.md) right after the prompt.

````text
I want to replace my TV's stock home screen with Clean TV and turn off the preinstalled bloat, and I'd like
you to walk me through it. Clean TV is an open-source launcher plus a set of ADB scripts:
https://github.com/hunterbrewer04/clean-tv

<my_setup>
TV: [brand and model, or "not sure"]
Computer: [Windows / macOS / Linux]
Terminal experience: [never used one / a little / comfortable]
</my_setup>

<source_of_truth>
The setup guide is https://raw.githubusercontent.com/hunterbrewer04/clean-tv/main/docs/GUIDE.md. Read it
before we start, and go by it over what you remember about Android TV, because the scripts and their checks
change between versions. If you can't open the link, say so and I'll paste the guide in. Once the repo is
cloned, if you can read its files, read a script before you tell me what it does.
</source_of_truth>

<done_looks_like>
Pressing Home on my remote opens Clean TV. The bloat batches I picked are disabled and
scripts/health-check.sh passes. Everything I use still works: my streaming apps are still signed in, and
casting, my remote, the TV's inputs and the Play Store all work. And I know scripts/restore.sh undoes all of it.
</done_looks_like>

<how_we_work>
Give me one step at a time: the command in a code block, one sentence on what it does and whether it
changes the TV, and what I should expect to see. Then wait for the output. If it shows FAIL, Refusing, an
error, or anything you didn't expect, stop there and figure out the cause with me. Those messages are
usually the scripts' safety checks doing their job.

If you can run commands yourself, go ahead with the read-only ones: survey.sh, health-check.sh, and any
script run without APPLY=1. Ask me before each command that has APPLY=1, because that's what changes the
TV, and tell me what it will change.

Some things only I can check, by looking at the TV: what Home opens, whether an app is still signed in,
whether casting works. After each change, ask me to check them rather than assuming.

Use plain words, and match the detail to my terminal experience above. If I ask for something risky, tell
me the risk and the safer way to get the same result.
</how_we_work>

<safety>
What I care about most is not losing my app logins and not ending up with a TV that won't work.

- Make every change to the TV through the repo's scripts. They refuse to touch protected packages
  (casting, remote, Play Services, inputs, keyboards, settings), check who owns the saved Wi-Fi, and log
  every change in a ledger so restore.sh can undo exactly that. Typing adb shell pm commands by hand skips
  all of it.
- Keep every change reversible. The scripts use pm disable-user, which restore.sh undoes. pm uninstall,
  pm clear and a factory reset can't be undone and sign me out of my apps, so when something goes wrong,
  the fix is restore.sh. If the TV boots into Android Recovery, the right choice is "Try again".
- Run each script without APPLY=1 first. That's a dry run that prints the plan, so walk me through it
  before we run it for real.
- Do one debloat batch at a time, then run health-check.sh and let me use the TV for a few minutes. That
  way, if something breaks, we know which batch did it.
- When a script refuses, fix the cause instead of working around it. For example, "owns N saved Wi-Fi
  networks" means disabling that package would delete those networks, maybe including the one the TV is
  on. The fix is to forget that network and join it again in the TV's Settings. Overrides like
  HOME_BLOCKERS_OK=1, or editing a profile, are only for a package we've looked into together.
- If survey.sh lists packages you can't identify, leave them enabled. Unknown vendor packages are often
  the TV's own system services.
</safety>

Start by asking about anything missing from <my_setup>. Then check that I have what the guide's "What you
need" section lists: adb, bash 4 or newer, and a copy of the repo.
````

## Tips

- If the assistant suggests a command that isn't one of the repo's scripts, ask it why.
- When something fails, the [troubleshooting table](GUIDE.md#troubleshooting) covers the common errors, and the assistant can read it too.
- If you got it working on a brand that isn't listed yet, a [profile report](https://github.com/hunterbrewer04/clean-tv/issues/new/choose) helps the next person.
