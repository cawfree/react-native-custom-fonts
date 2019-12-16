# react-native-custom-fonts
Use fonts specified via a network location, instead of managing them in your native builds!

<p align="center">
  <img src="./bin/out.gif" alt="react-native-custom-fonts" width="400" height="800">
</p>

## 🚀 Getting Started

Using [`npm`](https://www.npmjs.com/package/react-native-custom-fonts):

```bash
npm install --save react-native-custom-fonts
react-native link react-native-custom-fonts
```

Using [`yarn`](https://www.npmjs.com/package/react-native-custom-fonts):

```bash
yarn add react-native-custom-fonts
react-native link react-native-custom-fonts
```
## 😬 Contributing
Please report any [issues](https://github.com/cawfree/react-native-custom-fonts/issues) you come across, and feel free to [submit a Pull Request](https://github.com/cawfree/react-native-custom-fonts/pulls) if you'd like to add any enhancements. To make any changes, you can just branch from  `master`.

## 🤔 How does it work?

It's _really_ simple.

The problem is that usually, using custom fonts in a [React Native](https://github.com/facebook/react-native) application requires you to bundle the font files inside your app, and update your build settings to acknowledge the fonts actually exist. This means that every time you want to use a new (non-system) font, you are forced to recompile! This really breaks the flow of the  _hot reload_ style of development we've all come to know and love.

[react-native-custom-fonts](https://github.com/cawfree/react-native-custom-fonts) works around this limitation by accepting a declaration of remote font files resources, which are downloaded to your application's local storage. Once they're downloaded, they become available to your app.

Caching the fonts to your native device are managed by the [CustomFontsProvider](https://github.com/cawfree/react-native-custom-fonts/blob/e635afb8c333daae17f99f94e978f4b45910d361/index.js#L34), which you'll want to place t the root of your application.

Here's an example of how it works:

```javascript
import { CustomFontsProvider, Text, TextInput } from 'react-native-custom-fonts';

const fontFaces = [
  {
    fontFamily: 'Ubuntu',
    fontWeight: 'Normal',
    // Define the location of the font file. (In prod, this should be your cdn!)
    uri: 'https://raw.githubusercontent.com/opensourcedesign/fonts/master/ubuntu-font-family-0.80/Ubuntu-R.ttf',
  },
];

export default ({ ...nextProps }) => (
  <CustomFontsProvider
    fontFaces={fontFaces}
  >
    <Text
      style={{
        fontFamily: 'Ubuntu',
        fontWeight: 'normal',
        fontSize: 40,
      }}
    >
      {'I will be rendered using a dynamic font downloaded from the web!'}
    </Text>
    <TextInput
      style={{
        fontFamily: 'Ubuntu',
        fontWeight: 'normal',
        fontSize: 40,
      }}
      placeholder="I work for TextInputs, too!"
    />
  </CustomFontsProvider>
);

```

## 🔨 Manual installation

### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-custom-fonts` and add `RNCustomFonts.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCustomFonts.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import io.github.cawfree.RNCustomFontsPackage;` to the imports at the top of the file
  - Add `new RNCustomFontsPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-custom-fonts'
  	project(':react-native-custom-fonts').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-custom-fonts/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-custom-fonts')
  	```

## ✍️ Example 

Please check out the [example](https://github.com/cawfree/react-native-custom-fonts/blob/master/example/App.js) project for a full demonstration. Just `cd` into the directory, use `npm` or `yarn` to install the dependencies, then execute the app using the following:

```bash
react-native run-android # run on android
react-native run-ios     # run on ios
```

## 📌 Prop Types

### `CustomFontsProvider`
This is a React Context Provider for all children who were wrapped with a call to `ReactNativeCustomFonts.withCustomFont`. Manages the caching and assignment of remote fonts to children.

| Prop Name            | Data Type                                                                                                                                              | Required | Default    | Description                                                                                           |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------|-------------------------------------------------------------------------------------------------------|
| `fontFaces`          | propTypes.arrayOf(PropTypes.shape({   fontFamily: PropTypes.string.isRequired,   fontWeight: PropTypes.string,   uri: PropTypes.string.isRequired, })) | false    | []         | Defines the configuration of a remote font.                                                           |
| `latency`            | propTypes.number                                                                                                                                       | false    | 50         | Time in milliseconds to wait before the Provider attempts to assign a font to a child. (Android only) |
| `fadeDuration`       | propTypes.number                                                                                                                                       | false    | 250        | (iOS only)                                                                                            |
| `onDownloadDidStart` | propTypes.func                                                                                                                                         | false    | () => null | Callback for when the Provider begins downloading the fontFaces.                                      |
| `onDownloadDidEnd`   | propTypes.func                                                                                                                                         | false    | () => null | Callback for when the Provider has completed downloading the fontFaces.                               |


## ✌️ License
[MIT](https://opensource.org/licenses/MIT)

<p align="center">
  <a href="https://www.buymeacoffee.com/cawfree">
    <img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy @cawfree a coffee" width="232" height="50" />
  </a>
</p>
