# react-native-custom-fonts
Use dynamic fonts specified via a network location, instead of managing them in your native builds!

<p align="center">
  <img src="./bin/out.gif" alt="react-native-custom-fonts" width="400" height="800">
</p>

## 🚀 Getting Started

**>=0.60.0**

```bash
yarn add react-native-custom-fonts # or npm install --save react-native-custom-fonts
```

Then rebuild your app. On iOS, be sure to `pod install` your cocoapods in your app's `/ios` directory.

**<=0.59.X**

Using [`yarn`](https://www.npmjs.com/package/react-native-custom-fonts):

```bash
yarn add react-native-custom-fonts
react-native link react-native-custom-fonts
```

## Breaking Changes

  - **<1.2.0**
    - We've added a bunch of stability improvements, and migrated to a new [Hooks](https://reactjs.org/docs/hooks-intro.html)-based API.
    - The `fontFaces` array prop has been turned into a `fontFaces` object, whose keys are the _names_ of font styles you'd like to reference in your app.
    - To use a `fontFace`, you must specify the name in a call to `useCustomFont(name:String)`.

## ✍️ Example 

Please check out the [example](https://github.com/cawfree/react-native-custom-fonts/blob/master/example/App.js) project for a full demonstration. Just `cd` into the directory, use `npm` or `yarn` to install the dependencies, then execute the app using the following:

```bash
react-native run-android # run on android
react-native run-ios     # run on ios
```


### Simple <Text />

```javascript
import React from "react";
import PropTypes from "prop-types";
import {View, Text} from "react-native";
import CustomFontsProvider, {useCustomFont} from "react-native-custom-fonts";

const fontFaces = {
  // XXX: Specify the local name of your font. You'll use this to refer to it via the useCustomFont hook.
  'UbuntuBold': {
    uri: 'https://github.com/google/fonts/raw/master/ufl/ubuntu/Ubuntu-Bold.ttf',
    fontFamily: 'Ubuntu',
    fontWeight: 'bold',
    // XXX: You can also specify additional font styling.
    color: 'blue',
  },
};

const SomeComponent = () => {
  // Fetch the desired font by name. When the font has been cached, it will automatically update the View.
  const {...fontProps} = useCustomFont('UbuntuBold');
  return (
    <Text
      {...fontProps}
      children="Hello, world!"
    />
  );
};

export default () => (
  <CustomFontsProvider
    fontFaces={fontFaces}
  >
    <SomeComponent />
  </CustomFontsProvider>
);
```

### Where's my ref?

`react-native-custom-fonts` captures the `ref` prop of the `Text` component to make runtime property assignment. You can still access the ref, in one of two ways:

You can either **supply a ref**:

```javascript
const ref = useRef();
const {...fontProps} = useCustomFont('UbuntuBold', ref);
return (
  <Text
    ref={ref}
    {...fontProps}
  />
);
```

Or you can **use the provided ref**:

```javascript
const {ref, ...fontProps} = useCustomFont('UbuntuBold');
return (
  <Text
    ref={ref}
    {...fontProps}
  />
);
```

### Awesome, so what about additional styles?

It's possible to do this, too. Just fetch the `style` prop from the call to `useCustomFont`:

```javascript
const {style, ...fontProps} = useCustomFont('UbuntuBold');
return (
  <TextInput
    style={[style, {fontColor: 'blue'}]}
    {...fontProps}
  />
);
```

## 📌 Prop Types

### `CustomFontsProvider`
This is a React Context Provider for all children who were wrapped with a call to `ReactNativeCustomFonts.withCustomFont`. Manages the caching and assignment of remote fonts to children.

| Prop Name            | Data Type                                                                                                                                              | Required | Default    | Description                                                                                           |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------|-------------------------------------------------------------------------------------------------------|
| `fontFaces`          | propTypes.shape({}) | false    | {}         | Defines the configuration of the remote fonts.                                                           |
| `fallback`          | propTypes.shape({}) | false    | {color: 'red', fontWeight:'bold'}         | The style to use when font downloads fail.                                                           |
| `onDownloadDidStart` | propTypes.func                                                                                                                                         | false    | () => null | Callback for when the Provider begins downloading the fontFaces.                                      |
| `onDownloadDidEnd`   | propTypes.func                                                                                                                                         | false    | () => null | Callback for when the Provider has completed downloading the fontFaces.                               |
| `onDownloadDidError`   | propTypes.func                                                                                                                                         | false    | () => null | Called when an error has been thrown when downloading the fontFaces.                               |

## 😬 Contributing
Please report any [issues](https://github.com/cawfree/react-native-custom-fonts/issues) you come across, and feel free to [submit a Pull Request](https://github.com/cawfree/react-native-custom-fonts/pulls) if you'd like to add any enhancements. To make any changes, you can just branch from  `master`.

## ✌️ License
[MIT](https://opensource.org/licenses/MIT)

<p align="center">
  <a href="https://www.buymeacoffee.com/cawfree">
    <img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy @cawfree a coffee" width="232" height="50" />
  </a>
</p>
